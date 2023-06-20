package com.vladmihalcea.hpjp.util.providers.queries;

/**
 * @author Vlad Mihalcea
 */
public class MySQLQueries implements Queries {

    public static final Queries INSTANCE = new MySQLQueries();

    @Override
    public String transactionId() {
        return "SELECT tx.trx_id FROM information_schema.innodb_trx tx WHERE tx.trx_mysql_thread_id = connection_id()";
    }
}
