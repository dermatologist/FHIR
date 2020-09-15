/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.database.utils.postgresql;

import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ibm.fhir.database.utils.api.ConnectionDetails;
import com.ibm.fhir.database.utils.api.ConnectionException;
import com.ibm.fhir.database.utils.api.DataAccessException;
import com.ibm.fhir.database.utils.api.DuplicateSchemaException;
import com.ibm.fhir.database.utils.api.IDatabaseTranslator;
import com.ibm.fhir.database.utils.api.LockException;
import com.ibm.fhir.database.utils.api.UndefinedNameException;
import com.ibm.fhir.database.utils.api.UniqueConstraintViolationException;
import com.ibm.fhir.database.utils.common.DataDefinitionUtil;
import com.ibm.fhir.database.utils.model.DbType;

/**
 * translates database access to PostgreSql supported access.
 */
public class PostgreSqlTranslator implements IDatabaseTranslator {
    private static final Logger logger = Logger.getLogger(PostgreSqlTranslator.class.getName());

    @Override
    public String addForUpdate(String sql) {
        return sql + " FOR UPDATE";
    }

    @Override
    public boolean isDerby() {
        return false;
    }

    @Override
    public String globalTempTableName(String tableName) {
        return "SYSTEM." + tableName;
    }

    @Override
    public String createGlobalTempTable(String ddl) {
        return "DECLARE " + ddl;
    }

    @Override
    public boolean isDuplicate(SQLException x) {
        // Class Code 23: Constraint Violation
        // Refer to https://www.postgresql.org/docs/12/errcodes-appendix.html for more detail
        return "23505".equals(x.getSQLState());
    }

    @Override
    public boolean isAlreadyExists(SQLException x) {
        return "42710".equals(x.getSQLState());
    }

    @Override
    public boolean isLockTimeout(SQLException x) {
        return false;
    }

    @Override
    public boolean isDeadlock(SQLException x) {
        final String sqlState = x.getSQLState();
        return "40XL1".equals(sqlState) || "40XL2".equals(sqlState);
    }

    @Override
    public boolean isConnectionError(SQLException x) {
        String sqlState = x.getSQLState();
        return sqlState != null && sqlState.startsWith("08");
    }

    @Override
    public DataAccessException translate(SQLException x) {
        if (isDeadlock(x)) {
            return new LockException(x, true);
        } else if (isLockTimeout(x)) {
            return new LockException(x, false);
        } else if (isConnectionError(x)) {
            return new ConnectionException(x);
        } else if (isDuplicate(x)) {
            return new UniqueConstraintViolationException(x);
        } else if (isUndefinedName(x)) {
            return new UndefinedNameException(x);
        } else if(isDuplicateSchema(x)) {
            return new DuplicateSchemaException(x);
        } else {
            return new DataAccessException(x);
        }
    }

    /**
     * @implNote sometimes this is wrapped in a cause by.
     * @see https://www.postgresql.org/docs/9.4/errcodes-appendix.html
     */
    public boolean isDuplicateSchema(SQLException x) {
        Throwable inter = x.getCause();
        SQLException temp = null;
        if (inter instanceof SQLException) {
            temp = ((SQLException) inter);
        }
        return "42P06".equals(x.getSQLState()) || (temp != null && "42P06".equals(temp.getSQLState()));
    }

    @Override
    public boolean isUndefinedName(SQLException x) {
        return "42X05".equals(x.getSQLState());
    }

    @Override
    public void fillProperties(Properties p, ConnectionDetails cd) {
        p.put("user", cd.getUser());
        p.put("password", cd.getPassword());

        if (cd.isSsl()) {
            p.put("sslConnection", "true");
        }

        if (cd.isHA()) {
            logger.warning("No HA support for PostgreSql");
        }
    }

    @Override
    public String timestampDiff(String left, String right, String alias) {
        // diff is left - right, e.g. current - start time
        if (alias == null || alias.isEmpty()) {
            return String.format("EXTRACT(EPOCH FROM %s - %s)", left, right);
        }
        else {
            return String.format("EXTRACT(EPOCH FROM %s - %s) AS %s", left, right, alias);
        }
    }

    @Override
    public String createSequence(String name, int cache) {
        // cache isn't supported by PostgreSql
        return "CREATE SEQUENCE " + name;
    }

    @Override
    public String reorgTableCommand(String tableName) {
        // REORG TABLE not supported by PostgreSql
        throw new UnsupportedOperationException("reorg table is not supported!");
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getUrl(Properties connectionProperties) {
        PostgreSqlPropertyAdapter adapter = new PostgreSqlPropertyAdapter(connectionProperties);
        StringBuilder jdbcUrl = new StringBuilder();
        jdbcUrl.append("jdbc:postgresql://");
        jdbcUrl.append(adapter.getHost());
        jdbcUrl.append(':');
        jdbcUrl.append(adapter.getPort());
        jdbcUrl.append('/');
        jdbcUrl.append(adapter.getDatabase());

        // Filter out comments and db specific values
        // @see https://jdbc.postgresql.org/documentation/head/connect.html#ssl for more details on configuration options
        Map<Object, Object> entries = connectionProperties.entrySet()
                .stream().filter(p -> !p.getKey().toString().startsWith("db.")
                    && !p.getKey().toString().startsWith("#")
                    && !p.getKey().toString().equals("user")
                    && !p.getKey().toString().equals("password"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        boolean first = true;
        if(entries != null && !entries.isEmpty()) {
            jdbcUrl.append('?');
            // The following is really all strings and casted to such an object
            for(Entry<Object, Object> entry : entries.entrySet()){
                if (!first) {
                    jdbcUrl.append('&');
                } else {
                    first = false;
                }
                jdbcUrl.append(entry.getKey());
                jdbcUrl.append('=');
                jdbcUrl.append(entry.getValue());
            }
        }
        return jdbcUrl.toString();
    }

    @Override
    public boolean clobSupportsInline() {
        return false;
    }

    @Override
    public DbType getType() {
        return DbType.POSTGRESQL;
    }

    @Override
    public String dualTableName() {
        // PostgreSQL does not support a "DUAL" table because the FROM clause is optional.
        return null;
    }

    @Override
    public String selectSequenceNextValue(String schemaName, String sequenceName) {
        // PostgreSQL uses function-based syntax for sequences and FROM is not required
        String qname = DataDefinitionUtil.getQualifiedName(schemaName, sequenceName);
        return "SELECT nextval('" + qname + "')";
    }

    @Override
    public String currentTimestampString() {
        return "CURRENT_TIMESTAMP";
    }
    
    @Override
    public String dropForeignKeyConstraint(String qualifiedTableName, String constraintName) {
        // PostgreSQL syntax is not the same as DB2/Derby
        return "ALTER TABLE " + qualifiedTableName + " DROP CONSTRAINT IF EXISTS " + constraintName;
    }
}