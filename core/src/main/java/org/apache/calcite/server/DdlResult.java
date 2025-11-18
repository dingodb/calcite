package org.apache.calcite.server;

import java.sql.SQLWarning;

public class DdlResult {
    private SQLWarning sqlWarning;
    private long affectedRows;

    public DdlResult(SQLWarning sqlWarning, long affectedRows) {
        this.sqlWarning = sqlWarning;
        this.affectedRows = affectedRows;
    }

    public DdlResult() {

    }

    public void setSqlWarning(String warning) {
        this.sqlWarning = new SQLWarning(warning);
    }

    public void setSqlWarning(SQLWarning sqlWarning) {
        this.sqlWarning = sqlWarning;
    }

    public SQLWarning getSqlWarning() {
        return sqlWarning;
    }

    public long getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(long affectedRows) {
        this.affectedRows = affectedRows;
    }
}
