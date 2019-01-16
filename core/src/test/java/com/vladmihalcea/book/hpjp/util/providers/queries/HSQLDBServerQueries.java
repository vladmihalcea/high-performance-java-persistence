package com.vladmihalcea.book.hpjp.util.providers.queries;

/**
 * @author Vlad Mihalcea
 */
public class HSQLDBServerQueries implements Queries {

    public static final Queries INSTANCE = new HSQLDBServerQueries();

    @Override
    public String transactionId() {
        return "VALUES (TRANSACTION_ID())";
    }
}
