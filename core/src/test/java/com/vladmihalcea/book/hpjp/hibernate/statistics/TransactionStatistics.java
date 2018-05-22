package com.vladmihalcea.book.hpjp.hibernate.statistics;


import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.internal.StatisticsImpl;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Vlad Mihalcea
 */
public class TransactionStatistics extends StatisticsImpl {

    private static final ThreadLocal<AtomicLong> startNanos = ThreadLocal.withInitial(AtomicLong::new);

    private static final ThreadLocal<AtomicLong> connectionCounter = ThreadLocal.withInitial(AtomicLong::new);

    private StatisticsReport report = new StatisticsReport();

    public TransactionStatistics(SessionFactoryImplementor sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public void connect() {
        connectionCounter.get().incrementAndGet();
        startNanos.get().compareAndSet(0, System.nanoTime());
        super.connect();
    }

    @Override
    public void endTransaction(boolean success) {
        try {
            report.transactionTime(System.nanoTime() - startNanos.get().get());
            report.connectionsCount(connectionCounter.get().get());
            report.generate();
        } finally {
            startNanos.remove();
            connectionCounter.remove();
        }
        super.endTransaction(success);
    }
}
