package com.vladmihalcea.hpjp.spring.transaction.mdc.event;

import com.vladmihalcea.hpjp.util.TsidUtils;
import org.slf4j.MDC;

/**
 * @author Vlad Mihalcea
 */
class TransactionInfo {

    private final Long persistenceContextId;

    private String transactionId;

    private MDC.MDCCloseable mdc;

    public TransactionInfo() {
        this.persistenceContextId = TsidUtils.randomTsid().toLong();
        setMdc();
    }

    public boolean hasTransactionId() {
        return transactionId != null;
    }

    public TransactionInfo setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        setMdc();
        return this;
    }

    private void setMdc() {
        this.mdc = MDC.putCloseable(
            "txId",
            String.format(
                " Persistence Context Id: [%d], DB Transaction Id: [%s]",
                persistenceContextId,
                transactionId
            )
        );
    }

    public void close() {
        if(mdc != null) {
            mdc.close();
        }
    }
}
