package com.vladmihalcea.book.hpjp.util.providers.queries;

/**
 * @author Vlad Mihalcea
 */
public class MySQLQueries implements Queries {

    public static final Queries INSTANCE = new MySQLQueries();

    @Override
    public String transactionId() {
        return "SELECT trx_id FROM information_schema.innodb_trx";
    }
}
