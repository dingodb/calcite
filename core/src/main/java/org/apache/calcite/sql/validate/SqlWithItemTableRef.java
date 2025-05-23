package org.apache.calcite.sql.validate;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlTableRef;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * A <code>SqlWithItemTableRef</code> is a node created during validation for
 * recursive queries which represents a table reference in a {@code WITH RECURSIVE} clause.
 */
public class SqlWithItemTableRef extends SqlTableRef {
    private final SqlWithItem withItem;
    public SqlWithItemTableRef(SqlParserPos pos,
                               SqlWithItem withItem) {
        super(pos, withItem.name, SqlNodeList.EMPTY);
        this.withItem = withItem;
    }

    private static final SqlOperator OPERATOR =
            new SqlSpecialOperator("WITH_ITEM_TABLE_REF", SqlKind.WITH_ITEM_TABLE_REF) {
                @Override public SqlCall createCall(
                        @Nullable SqlLiteral functionQualifier,
                        SqlParserPos pos, @Nullable SqlNode... operands) {
                    return new SqlWithItemTableRef(pos,
                            (SqlWithItem) requireNonNull(operands[0], "withItem"));
                }
            };
    @Override public SqlOperator getOperator() {
        return OPERATOR;
    }

    public SqlWithItem getWithItem() {
        return withItem;
    }
}
