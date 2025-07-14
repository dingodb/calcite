package org.apache.calcite.sql.fun;

import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlSplittableAggFunction;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;

public class SqlInternalFirstValueAggFunction extends SqlAggFunction {
    public SqlInternalFirstValueAggFunction() {
        super(
                "__FIRST_VALUE",
                null,
                SqlKind.__FIRST_VALUE,
                ReturnTypes.ARG0,
                null,
                OperandTypes.ANY,
                SqlFunctionCategory.SYSTEM,
                false,
                false);
    }

    @Override public void unparse(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        call.operand(0).unparse(writer, leftPrec, rightPrec);
    }

    @Override public <T> T unwrap(Class<T> clazz) {
        if (clazz == SqlSplittableAggFunction.class) {
            return clazz.cast(SqlSplittableAggFunction.SelfSplitter.INSTANCE);
        }
        return super.unwrap(clazz);
    }
}
