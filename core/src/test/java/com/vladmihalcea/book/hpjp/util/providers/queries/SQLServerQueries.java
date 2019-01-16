package com.vladmihalcea.book.hpjp.util.providers.queries;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerQueries implements Queries {

    public static final Queries INSTANCE = new SQLServerQueries();

    @Override
    public String transactionId() {
        return "SELECT CONVERT(VARCHAR, CURRENT_TRANSACTION_ID())";
    }
}
