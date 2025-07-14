package org.apache.calcite.util;

import java.util.HashSet;
import java.util.Set;

public class EqualsContext {
    public static final EqualsContext DEFAULT_EQUALS_CONTEXT = new EqualsContext();

    final private boolean genColSubstitute;
    final private Set<Integer> constantParamIndex;
    final String schemaName;
    final String tableName;
    final String idTableName;

    public EqualsContext() {
        this.genColSubstitute = false;
        this.constantParamIndex = new HashSet<>();
        this.schemaName = "";
        this.tableName = "";
        this.idTableName = "";
    }

    public EqualsContext(String schemaName, String tableName, String idTableName) {
        this.genColSubstitute = true;
        this.constantParamIndex = new HashSet<>();
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.idTableName = idTableName;
    }

    public boolean isGenColSubstitute() {
        return genColSubstitute;
    }

    public Set<Integer> getConstantParamIndex() {
        return constantParamIndex;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getIdTableName() {
        return idTableName;
    }
}
