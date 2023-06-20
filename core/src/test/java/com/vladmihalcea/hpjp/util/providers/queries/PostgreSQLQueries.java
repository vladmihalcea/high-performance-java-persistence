package com.vladmihalcea.hpjp.util.providers.queries;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLQueries implements Queries {

    public static final Queries INSTANCE = new PostgreSQLQueries();

    @Override
    public String transactionId() {
        return "SELECT CAST(pg_current_xact_id_if_assigned() AS text)";
    }
}
