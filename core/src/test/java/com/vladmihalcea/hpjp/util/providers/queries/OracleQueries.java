package com.vladmihalcea.hpjp.util.providers.queries;

/**
 * @author Vlad Mihalcea
 */
public class OracleQueries implements Queries {

    public static final Queries INSTANCE = new OracleQueries();

    @Override
    public String transactionId() {
        return """
            SELECT RAWTOHEX(tx.xid)
            FROM v$transaction tx
            JOIN v$session s ON tx.addr=s.taddr
            WHERE s.sid = sys_context('userenv','sid')
            """ ;
    }
}
