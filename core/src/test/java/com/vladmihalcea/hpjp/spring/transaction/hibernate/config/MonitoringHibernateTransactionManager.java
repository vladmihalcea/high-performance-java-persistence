package com.vladmihalcea.hpjp.spring.transaction.hibernate.config;

import com.vladmihalcea.hpjp.util.StackTraceUtils;
import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.util.concurrent.atomic.LongAdder;

public class MonitoringHibernateTransactionManager extends HibernateTransactionManager {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private static ThreadLocal<LongAdder> statementCounterHolder = new ThreadLocal<>();

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        Session session = getSessionFactory().getCurrentSession();
        final LongAdder statementCounter = new LongAdder();
        statementCounterHolder.set(statementCounter);
        session.addEventListeners(new BaseSessionEventListener() {
            @Override
            public void jdbcPrepareStatementStart() {
                statementCounter.increment();
            }
        });
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        LongAdder statementCounter = statementCounterHolder.get();
        if (statementCounter.intValue() == 0) {
            LOGGER.warn(
                "Current transactional method {} didn't execute any SQL statement",
                StackTraceUtils.stackTracePath(
                    StackTraceUtils.stackTraceElements(
                        "com.vladmihalcea"
                    )
                )
            );
        }
        statementCounterHolder.remove();
        super.doCleanupAfterCompletion(transaction);
    }
}
