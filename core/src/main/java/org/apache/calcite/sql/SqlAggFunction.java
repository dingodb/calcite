/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.linq4j.function.Experimental;
import org.apache.calcite.plan.Context;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.fun.SqlAvgAggFunction;
import org.apache.calcite.sql.fun.SqlBasicAggFunction;
import org.apache.calcite.sql.fun.SqlCastFunction;
import org.apache.calcite.sql.fun.SqlSumAggFunction;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlOperandTypeChecker;
import org.apache.calcite.sql.type.SqlOperandTypeInference;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.type.SqlTypeUtil;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.calcite.sql.validate.implicit.TypeCoercion;
import org.apache.calcite.util.Optionality;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for the definition of an aggregate function: an operator
 * which aggregates sets of values into a result.
 *
 * @see SqlBasicAggFunction
 */
public abstract class SqlAggFunction extends SqlFunction implements Context {
  private final boolean requiresOrder;
  private final boolean requiresOver;
  private final Optionality requiresGroupOrder;

  //~ Constructors -----------------------------------------------------------

  /** Creates a built-in SqlAggFunction. */
  @Deprecated // to be removed before 2.0
  protected SqlAggFunction(
      String name,
      SqlKind kind,
      SqlReturnTypeInference returnTypeInference,
      @Nullable SqlOperandTypeInference operandTypeInference,
      @Nullable SqlOperandTypeChecker operandTypeChecker,
      SqlFunctionCategory funcType) {
    // We leave sqlIdentifier as null to indicate that this is a builtin.
    this(name, null, kind, returnTypeInference, operandTypeInference,
        operandTypeChecker, funcType, false, false,
        Optionality.FORBIDDEN);
  }

  /** Creates a user-defined SqlAggFunction. */
  @Deprecated // to be removed before 2.0
  protected SqlAggFunction(
      String name,
      @Nullable SqlIdentifier sqlIdentifier,
      SqlKind kind,
      SqlReturnTypeInference returnTypeInference,
      @Nullable SqlOperandTypeInference operandTypeInference,
      @Nullable SqlOperandTypeChecker operandTypeChecker,
      SqlFunctionCategory funcType) {
    this(name, sqlIdentifier, kind, returnTypeInference, operandTypeInference,
        operandTypeChecker, funcType, false, false,
        Optionality.FORBIDDEN);
  }

  @Deprecated // to be removed before 2.0
  protected SqlAggFunction(
      String name,
      @Nullable SqlIdentifier sqlIdentifier,
      SqlKind kind,
      SqlReturnTypeInference returnTypeInference,
      @Nullable SqlOperandTypeInference operandTypeInference,
      @Nullable SqlOperandTypeChecker operandTypeChecker,
      SqlFunctionCategory funcType,
      boolean requiresOrder,
      boolean requiresOver) {
    this(name, sqlIdentifier, kind, returnTypeInference, operandTypeInference,
        operandTypeChecker, funcType, requiresOrder, requiresOver,
        Optionality.FORBIDDEN);
  }

  /** Creates a built-in or user-defined SqlAggFunction or window function.
   *
   * <p>A user-defined function will have a value for {@code sqlIdentifier}; for
   * a built-in function it will be null. */
  protected SqlAggFunction(
      String name,
      @Nullable SqlIdentifier sqlIdentifier,
      SqlKind kind,
      SqlReturnTypeInference returnTypeInference,
      @Nullable SqlOperandTypeInference operandTypeInference,
      @Nullable SqlOperandTypeChecker operandTypeChecker,
      SqlFunctionCategory funcType,
      boolean requiresOrder,
      boolean requiresOver,
      Optionality requiresGroupOrder) {
    super(name, sqlIdentifier, kind, returnTypeInference, operandTypeInference,
        operandTypeChecker, funcType);
    this.requiresOrder = requiresOrder;
    this.requiresOver = requiresOver;
    this.requiresGroupOrder = Objects.requireNonNull(requiresGroupOrder, "requiresGroupOrder");
  }

  //~ Methods ----------------------------------------------------------------

  @Override public <T extends Object> @Nullable T unwrap(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : null;
  }

  @Override public boolean isAggregator() {
    return true;
  }

  @Override public boolean isQuantifierAllowed() {
    return true;
  }

  @Override public void validateCall(
      SqlCall call,
      SqlValidator validator,
      SqlValidatorScope scope,
      SqlValidatorScope operandScope) {
    super.validateCall(call, validator, scope, operandScope);
    validator.validateAggregateParams(call, null, null, null, scope);
  }

  @Override public final boolean requiresOrder() {
    return requiresOrder;
  }

  /** Returns whether this aggregate function must, may, or must not contain a
   * {@code WITHIN GROUP (ORDER ...)} clause.
   *
   * <p>Cases:<ul>
   *
   * <li>If {@link Optionality#MANDATORY},
   * then {@code AGG(x) WITHIN GROUP (ORDER BY 1)} is valid,
   * and {@code AGG(x)} is invalid.
   *
   * <li>If {@link Optionality#OPTIONAL},
   * then {@code AGG(x) WITHIN GROUP (ORDER BY 1)}
   * and {@code AGG(x)} are both valid.
   *
   * <li>If {@link Optionality#IGNORED},
   * then {@code AGG(x)} is valid,
   * and {@code AGG(x) WITHIN GROUP (ORDER BY 1)} is valid but is
   * treated the same as {@code AGG(x)}.
   *
   * <li>If {@link Optionality#FORBIDDEN},
   * then {@code AGG(x) WITHIN GROUP (ORDER BY 1)} is invalid,
   * and {@code AGG(x)} is valid.
   * </ul>
   */
  public Optionality requiresGroupOrder() {
    return requiresGroupOrder;
  }

  @Override public final boolean requiresOver() {
    return requiresOver;
  }

    @Override
    protected List<RelDataType> constructArgTypeList(
        SqlValidator validator,
        SqlValidatorScope scope,
        SqlCall call,
        List<SqlNode> args,
        boolean convertRowArgToColumnList) {
        // Scope for operands. Usually the same as 'scope'.
        final SqlValidatorScope operandScope = scope.getOperandScope(call);

        final ImmutableList.Builder<RelDataType> argTypeBuilder =
            ImmutableList.builder();
        for (SqlNode operand : args) {
            RelDataType nodeType;
            // for row arguments that should be converted to ColumnList
            // types, set the nodeType to a ColumnList type but defer
            // validating the arguments of the row constructor until we know
            // for sure that the row argument maps to a ColumnList type
            if (operand.getKind() == SqlKind.ROW && convertRowArgToColumnList) {
                RelDataTypeFactory typeFactory = validator.getTypeFactory();
                nodeType = typeFactory.createSqlType(SqlTypeName.COLUMN_LIST);
                validator.setValidatedNodeType(operand, nodeType);
                argTypeBuilder.add(nodeType);
            } else {
                nodeType = validator.deriveType(operandScope, operand);

                if(getClass() == SqlSumAggFunction.class) {
                    RelDataTypeFactory typeFactory = validator.getTypeFactory();
                    TypeCoercion typeCoercion = validator.getTypeCoercion();
                    SqlFunction castFunction = new SqlCastFunction();
                    SqlTypeName typeName = nodeType.getSqlTypeName();

                    if(typeName == SqlTypeName.TINYINT || typeName == SqlTypeName.INTEGER
                        || typeName == SqlTypeName.BIGINT || typeName == SqlTypeName.DECIMAL) {
                        int precision = 0;
                        int scale = 0;
                        boolean nullable = nodeType.isNullable();

                        if (nodeType.getSqlTypeName() == SqlTypeName.TINYINT) {
                            precision = 25;
                        } else if(nodeType.getSqlTypeName() == SqlTypeName.INTEGER) {
                            precision = 32;
                        } else if(nodeType.getSqlTypeName() == SqlTypeName.BIGINT) {
                            precision = 41;
                        } else if(nodeType.getSqlTypeName() == SqlTypeName.DECIMAL) {
                            precision = nodeType.getPrecision() + 22;
                            precision = precision > 65 ? 65 : precision;
                            scale = nodeType.getScale();
                        }

                        RelDataType targetType = typeFactory.createSqlType(SqlTypeName.DECIMAL,precision,scale,nullable);
                        SqlNode sqlNode = castFunction.createCall(SqlParserPos.ZERO, operand,
                            SqlTypeUtil.convertTypeToSpec(targetType).withNullable(targetType.isNullable()));
                        call.setOperand(0, sqlNode);
                        argTypeBuilder.add(targetType);
                    } else if (typeName == SqlTypeName.FLOAT ) {
                        RelDataType targetType = typeFactory.createSqlType(SqlTypeName.DOUBLE);

                        SqlNode sqlNode = castFunction.createCall(SqlParserPos.ZERO, operand,
                                SqlTypeUtil.convertTypeToSpec(targetType).withNullable(targetType.isNullable()));
                        call.setOperand(0, sqlNode);
                        argTypeBuilder.add(targetType);
                    } else {
                        argTypeBuilder.add(nodeType);
                    }
                } else if(getClass() == SqlAvgAggFunction.class) {
                    RelDataTypeFactory typeFactory = validator.getTypeFactory();
                    TypeCoercion typeCoercion = validator.getTypeCoercion();
                    SqlFunction castFunction = new SqlCastFunction();
                    SqlTypeName typeName = nodeType.getSqlTypeName();

                    if(typeName == SqlTypeName.TINYINT || typeName == SqlTypeName.INTEGER
                        || typeName == SqlTypeName.BIGINT || typeName == SqlTypeName.DECIMAL) {
                        int precision = 0;
                        int scale = 0;
                        boolean nullable = nodeType.isNullable();

                        if (nodeType.getSqlTypeName() == SqlTypeName.TINYINT) {
                            precision = 7;
                            scale = 4;
                        } else if(nodeType.getSqlTypeName() == SqlTypeName.INTEGER) {
                            precision = 14;
                            scale = 4;
                        } else if(nodeType.getSqlTypeName() == SqlTypeName.BIGINT) {
                            precision = 23;
                            scale = 4;
                        } else if(nodeType.getSqlTypeName() == SqlTypeName.DECIMAL) {
                            precision = nodeType.getPrecision() + 4;
                            scale = nodeType.getScale() + 4;
                        }

                        int scale1 = (scale >= 4) ? scale - 4 : scale;
                        RelDataType targetType = typeFactory.createSqlType(SqlTypeName.DECIMAL,precision,scale,nullable);
                        RelDataType targetType1 = typeFactory.createSqlType(SqlTypeName.DECIMAL,precision,scale1,nullable);
                        SqlNode sqlNode = castFunction.createCall(SqlParserPos.ZERO, operand,
                            SqlTypeUtil.convertTypeToSpec(targetType1).withNullable(targetType1.isNullable()));
                        call.setOperand(0, sqlNode);
                        argTypeBuilder.add(targetType);
                    } else if (typeName == SqlTypeName.FLOAT ) {
                        RelDataType targetType = typeFactory.createSqlType(SqlTypeName.DOUBLE);

                        SqlNode sqlNode = castFunction.createCall(SqlParserPos.ZERO, operand,
                                SqlTypeUtil.convertTypeToSpec(targetType).withNullable(targetType.isNullable()));
                        call.setOperand(0, sqlNode);
                        argTypeBuilder.add(targetType);
                    } else {
                        argTypeBuilder.add(nodeType);
                    }
                } else {
                    argTypeBuilder.add(nodeType);
                }
            }
        }

        return argTypeBuilder.build();
    }

  /** Returns whether this aggregate function allows the {@code DISTINCT}
   * keyword.
   *
   * <p>The default implementation returns {@link Optionality#OPTIONAL},
   * which is appropriate for most aggregate functions, including {@code SUM}
   * and {@code COUNT}.
   *
   * <p>Some aggregate functions, for example {@code MIN}, produce the same
   * result with or without {@code DISTINCT}, and therefore return
   * {@link Optionality#IGNORED} to indicate this. For such functions,
   * Calcite will probably remove {@code DISTINCT} while optimizing the query.
   */
  public Optionality getDistinctOptionality() {
    return Optionality.OPTIONAL;
  }

  @Deprecated // to be removed before 2.0
  public List<RelDataType> getParameterTypes(RelDataTypeFactory typeFactory) {
    throw new UnsupportedOperationException("remove before calcite-2.0");
  }

  @Deprecated // to be removed before 2.0
  public RelDataType getReturnType(RelDataTypeFactory typeFactory) {
    throw new UnsupportedOperationException("remove before calcite-2.0");
  }

  /** Whether this aggregate function allows a {@code FILTER (WHERE ...)}
   * clause. */
  public boolean allowsFilter() {
    return true;
  }

  /** Returns whether this aggregate function allows specifying null treatment
   * ({@code RESPECT NULLS} or {@code IGNORE NULLS}). */
  public boolean allowsNullTreatment() {
    return false;
  }

  /**
   * Gets rollup aggregation function.
   */
  public @Nullable SqlAggFunction getRollup() {
    return null;
  }

  /** Returns whether this aggregate function is a PERCENTILE function.
   * Such functions require a {@code WITHIN GROUP} clause that has precisely
   * one sort key.
   *
   * <p>NOTE: This API is experimental and subject to change without notice. */
  @Experimental
  public boolean isPercentile() {
    return false;
  }
}
