package org.apache.calcite.rel.metadata;

import com.google.common.collect.ImmutableSet;
import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.*;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexVisitor;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.util.BuiltInMethod;

import java.util.*;
import java.util.stream.Collectors;

public class RelMdDmlColumnNames implements MetadataHandler<BuiltInMetadata.DmlColumnName> {

    public static final RelMetadataProvider SOURCE =
            ReflectiveRelMetadataProvider.reflectiveSource(BuiltInMethod.DML_COLUMN_NAME.method,
                    new RelMdDmlColumnNames());

    @Override
    public MetadataDef<BuiltInMetadata.DmlColumnName> getDef() {
        return BuiltInMetadata.DmlColumnName.DEF;
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(Aggregate rel, RelMetadataQuery mq) {
        final List<Set<RelColumnOrigin>> origins = mq.getDmlColumnNames(rel.getInput());

        if (null == origins) {
            return null;
        }

        List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (int iOutputColumn = 0; iOutputColumn < rel.getRowType().getFieldCount(); iOutputColumn++) {
            if (iOutputColumn < rel.getGroupCount()) {
                // Group columns pass through directly.
                result.add(origins.get(iOutputColumn));
                continue;
            }

            if (rel.indicator) {
                if (iOutputColumn < rel.getGroupCount() + rel.getIndicatorCount()) {
                    // The indicator column is originated here.
                    result.add(ImmutableSet.of());
                    continue;
                }
            }

            // Aggregate columns are derived from input columns
            AggregateCall call = rel.getAggCallList()
                    .get(iOutputColumn - rel.getGroupCount() - rel.getIndicatorCount());

            final Set<RelColumnOrigin> set = new HashSet<>();
            for (Integer iInput : call.getArgList()) {
                Set<RelColumnOrigin> inputSet = origins.get(iInput);
                inputSet = createDerivedColumnOrigins(inputSet);
                if (inputSet != null) {
                    set.addAll(inputSet);
                }
            }
            result.add(set);
        }
        return result;
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(Join rel, RelMetadataQuery mq) {
        final RelNode left = rel.getLeft();
        final RelNode right = rel.getRight();
        final int nLeftColumns = left.getRowType().getFieldList().size();

        List<Set<RelColumnOrigin>> leftOrigins;
        if (left instanceof Join || left instanceof TableScan) {
            leftOrigins = mq.getDmlColumnNames(left);
        } else {
            // Build columnNames from row type
            leftOrigins = columnOriginForSubquery(left);
        }

        List<Set<RelColumnOrigin>> rightOrigins;
        if (right instanceof Join || right instanceof TableScan) {
            rightOrigins = mq.getDmlColumnNames(right);
        } else {
            // Build columnNames from row type
            rightOrigins = columnOriginForSubquery(right);
        }

        if (null == leftOrigins || null == rightOrigins) {
            return null;
        }

        List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (int ci = 0; ci < rel.getRowType().getFieldCount(); ci++) {
            Set<RelColumnOrigin> set;
            if (ci < nLeftColumns) {
                set = leftOrigins.get(ci);
                // null generation does not change column name
            } else {
                set = rightOrigins.get(ci - nLeftColumns);
                // null generation does not change column name
            }

            result.add(set);
        }
        return result;
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(Correlate rel, RelMetadataQuery mq) {
        final int nLeftColumns = rel.getLeft().getRowType().getFieldList().size();

        final List<Set<RelColumnOrigin>> leftOrigins = mq.getDmlColumnNames(rel.getLeft());
        final List<Set<RelColumnOrigin>> rightOrigins = mq.getDmlColumnNames(rel.getRight());

        if (null == leftOrigins || null == rightOrigins) {
            return null;
        }

        List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (int ci = 0; ci < rel.getRowType().getFieldCount(); ci++) {
            Set<RelColumnOrigin> set;
            if (ci < nLeftColumns) {
                set = leftOrigins.get(ci);
                // null generation does not change column name
            } else {
                set = rightOrigins.get(ci - nLeftColumns);
                // null generation does not change column name
            }

            result.add(set);
        }
        return result;
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(SetOp rel, RelMetadataQuery mq) {
        final List<Set<RelColumnOrigin>> set = new ArrayList<>();
        for (RelNode input : rel.getInputs()) {
            List<Set<RelColumnOrigin>> inputSet = mq.getDmlColumnNames(input);
            if (inputSet == null) {
                return null;
            }

            for (int ci = 0; ci < inputSet.size(); ci++) {
                if (set.size() <= ci) {
                    set.add(new HashSet<>());
                }

                set.get(ci).addAll(inputSet.get(ci));
            }
        }
        return set;
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(Project rel, final RelMetadataQuery mq) {
        final RelNode input = rel.getInput();

        final List<Set<RelColumnOrigin>> origins = mq.getDmlColumnNames(input);

        if (null == origins) {
            return null;
        }

        final List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (RexNode rexNode : rel.getProjects()) {
            Set<RelColumnOrigin> columnOrigins = null;
            if (rexNode instanceof RexInputRef) {
                // Direct reference: no derivation added.
                final RexInputRef inputRef = (RexInputRef) rexNode;
                columnOrigins = origins.get(inputRef.getIndex());
            } else {
                // Anything else is a derivation, possibly from multiple
                // columns.
                final Set<RelColumnOrigin> set = new HashSet<>();
                final RexVisitor<Void> visitor = new RexVisitorImpl<Void>(true) {

                    @Override
                    public Void visitInputRef(RexInputRef inputRef) {
                        set.addAll(origins.get(inputRef.getIndex()));
                        return null;
                    }
                };
                rexNode.accept(visitor);

                columnOrigins = createDerivedColumnOrigins(set);
            }

            result.add(columnOrigins);
        }

        return result;
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(Filter rel, RelMetadataQuery mq) {
        return mq.getDmlColumnNames(rel.getInput());
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(Sort rel, RelMetadataQuery mq) {
        return mq.getDmlColumnNames(rel.getInput());
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(Exchange rel, RelMetadataQuery mq) {
        return mq.getDmlColumnNames(rel.getInput());
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(TableFunctionScan rel, RelMetadataQuery mq) {
        final Set<RelColumnMapping> mappings = rel.getColumnMappings();
        if (mappings == null) {
            if (rel.getInputs().size() > 0) {
                // This is a non-leaf transformation: say we don't
                // know about origins, because there are probably
                // columns below.
                return null;
            } else {
                // This is a leaf transformation: say there are for sure no
                // column origins.
                return emptyColumnOrigin(rel);
            }
        }

        final List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (RelColumnMapping mapping : mappings) {
            final RelNode input = rel.getInputs().get(mapping.iInputRel);
            final int column = mapping.iInputColumn;

            final List<Set<RelColumnOrigin>> origins = mq.getDmlColumnNames(input);
            if (origins == null || origins.size() <= column) {
                return null;
            }

            Set<RelColumnOrigin> origin = origins.get(column);
            if (mapping.derived) {
                origin = createDerivedColumnOrigins(origin);
            }
            result.add(origin);
        }
        return result;
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(RelSubset rel, RelMetadataQuery mq) {
        return mq.getDmlColumnNames(rel.getOriginal());
    }

    public List<Set<RelColumnOrigin>> getDmlColumnNames(HepRelVertex rel, RelMetadataQuery mq) {
        return mq.getDmlColumnNames(rel.getCurrentRel());
    }

    // Catch-all rule when none of the others apply.
    public List<Set<RelColumnOrigin>> getDmlColumnNames(RelNode rel, RelMetadataQuery mq) {
        // NOTE jvs 28-Mar-2006: We may get this wrong for a physical table
        // expression which supports projections. In that case,
        // it's up to the plugin writer to override with the
        // correct information.

        if (rel.getInputs().size() > 0) {
            // No generic logic available for non-leaf rels.
            return null;
        }

        RelOptTable table = rel.getTable();
        if (table == null) {
            // Somebody is making column values up out of thin air, like a
            // VALUES clause, so we return empty set for each column in row
            // type.
            return emptyColumnOrigin(rel);

        }

        // Detect the case where a physical table expression is performing
        // projection, and say we don't know instead of making any assumptions.
        // (Theoretically we could try to map the projection using column
        // names.) This detection assumes the table expression doesn't handle
        // rename as well.
        if (table.getRowType() != rel.getRowType()) {
            return null;
        }

        return table.getRowType()
                .getFieldList()
                .stream()
                .map(field -> ImmutableSet.of(new RelColumnOrigin(table, field.getIndex(), false)))
                .collect(Collectors.toList());
    }

    public List<Set<RelColumnOrigin>> columnOriginForSubquery(RelNode rel) {
        return Ord.zip(rel.getRowType()
            .getFieldList())
            .stream()
            .map(o -> ImmutableSet.<RelColumnOrigin>of(new RelDmlColumnOrigin(rel, o.i, o.e.getName())))
            .collect(Collectors.toList());
    }

    public List<Set<RelColumnOrigin>> emptyColumnOrigin(RelNode rel) {
        return rel.getRowType()
            .getFieldList()
            .stream()
            .map(field -> ImmutableSet.<RelColumnOrigin>of())
            .collect(Collectors.toList());
    }

    private Set<RelColumnOrigin> createDerivedColumnOrigins(Set<RelColumnOrigin> inputSet) {
        if (inputSet == null) {
            return null;
        }
        final Set<RelColumnOrigin> set = new HashSet<>();
        for (RelColumnOrigin rco : inputSet) {
            RelColumnOrigin derived = new RelColumnOrigin(rco.getOriginTable(), rco.getOriginColumnOrdinal(), true);
            set.add(derived);
        }
        return set;
    }

    public static class RelDmlColumnOrigin extends RelColumnOrigin {
        private final RelNode rel;
        private final String columnName;

        public RelDmlColumnOrigin(RelNode rel, int iOriginColumn, String columnName) {
            super(null, iOriginColumn, true);
            this.columnName = columnName;
            this.rel = rel;
        }

        @Override
        public String getColumnName() {
            return columnName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RelDmlColumnOrigin)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            RelDmlColumnOrigin that = (RelDmlColumnOrigin) o;
            return Objects.equals(rel, that.rel) && Objects.equals(columnName, that.columnName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), rel, columnName);
        }
    }
}
