package org.apache.calcite.sql.validate;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlUtil;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.calcite.util.EqualsContext;
import org.apache.calcite.util.Litmus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.apache.calcite.util.Static.RESOURCE;

public class FirstValueAggReplacer extends SqlScopedShuttle {
    private final Deque<SqlValidatorScope> scopes = new ArrayDeque<>();
    private final List<SqlNode> extraExprs;
    private final List<SqlNode> groupExprs;
    private boolean distinct;
    private SqlValidatorImpl validator;
    private boolean isNotGroup;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates an FirstAggReplacer.
     *
     * @param validator  Validator
     * @param scope      Scope
     * @param groupExprs Expressions in GROUP BY (or SELECT DISTINCT) clause,
     *                   that are therefore available
     * @param distinct   Whether aggregation checking is because of a SELECT
     *                   DISTINCT clause
     */
    FirstValueAggReplacer(
            SqlValidatorImpl validator,
            AggregatingScope scope,
            List<SqlNode> extraExprs,
            List<SqlNode> groupExprs,
            boolean distinct) {
        super(scope);
        this.validator = validator;
        this.extraExprs = extraExprs;
        this.groupExprs = groupExprs;
        this.distinct = distinct;
        this.scopes.push(scope);
    }

    //~ Methods ----------------------------------------------------------------

    boolean isGroupExpr(SqlNode expr) {
        for (SqlNode groupExpr : groupExprs) {
            if (groupExpr.equalsDeep(expr, Litmus.IGNORE)) {
                return true;
            }
        }

        for (SqlNode extraExpr : extraExprs) {
            if (extraExpr.equalsDeep(expr, Litmus.IGNORE)) {
                return true;
            }
        }
        return false;
    }

    @Override public SqlNode visit(SqlIdentifier id) {
        if (isGroupExpr(id) || id.isStar()) {
            // Star may validly occur in "SELECT COUNT(*) OVER w"
            return id;
        }

        // Is it a call to a parentheses-free function?
        // todo
        SqlCall call =
                SqlUtil.makeCall(
                        validator.getOperatorTable(),
                        id);
        if (call != null) {
            return call.accept(this);
        }

        final SqlQualified fqId = scopes.peek().fullyQualify(id);
        if (isGroupExpr(fqId.identifier)) {
            return id;
        }
        /**
         * Should not throw exception for CoronaDB
         * we are compatible with MySQL
         */
        return SqlStdOperatorTable.__FIRST_VALUE.createCall(id.getParserPosition(), id);
    }

    public SqlNode visitScoped(SqlCall call) {
        final SqlValidatorScope scope = scopes.peek();
        if (call.getOperator().isAggregator()) {
            if (distinct) {
                if (scope instanceof AggregatingSelectScope) {
                    SqlNodeList selectList =
                            ((SqlSelect) scope.getNode()).getSelectList();

                    // Check if this aggregation function is just an element in the select
                    for (SqlNode sqlNode : selectList) {
                        if (sqlNode.getKind() == SqlKind.AS) {
                            sqlNode = ((SqlCall) sqlNode).operand(0);
                        }

                        if (validator.expand(sqlNode, scope)
                                .equalsDeep(call, Litmus.IGNORE)) {
                            return call;
                        }
                    }
                }

                // Cannot use agg fun in ORDER BY clause if have SELECT DISTINCT.
                SqlNode originalExpr = validator.getOriginal(call);
                final String exprString = originalExpr.toString();
                throw validator.newValidationError(call,
                        RESOURCE.notSelectDistinctExpr(exprString));
            }

            // For example, 'sum(sal)' in 'SELECT sum(sal) FROM emp GROUP
            // BY deptno'
            return call;
        }

        if (isGroupExpr(call)) {
            // This call matches an expression in the GROUP BY clause.
            return call;
        }

        final SqlCall groupCall =
                SqlStdOperatorTable.convertAuxiliaryToGroupCall(call);
        if (groupCall != null) {
            if (isGroupExpr(groupCall)) {
                // This call is an auxiliary function that matches a group call in the
                // GROUP BY clause.
                //
                // For example TUMBLE_START is an auxiliary of the TUMBLE
                // group function, and
                //   TUMBLE_START(rowtime, INTERVAL '1' HOUR)
                // matches
                //   TUMBLE(rowtime, INTERVAL '1' HOUR')
                return call;
            }
            throw validator.newValidationError(groupCall,
                    RESOURCE.auxiliaryWithoutMatchingGroupCall(
                            call.getOperator().getName(), groupCall.getOperator().getName()));
        }

        if (call.isA(SqlKind.QUERY)) {
            // Allow queries for now, even though they may contain
            // references to forbidden columns.
            return call;
        }

        // Switch to new scope.
        SqlValidatorScope newScope = scope.getOperandScope(call);
        scopes.push(newScope);
        ArgHandler<SqlNode> argHandler =
                new SqlShuttle.CallCopyingArgHandler(call, false);
        // Visit the operands (only expressions).
        call.getOperator()
                .acceptCall(this, call, true, argHandler);
        final SqlNode result = argHandler.result();
        // Restore scope.
        validator.setOriginal(result, call);
        scopes.pop();
        return result;
    }
}
