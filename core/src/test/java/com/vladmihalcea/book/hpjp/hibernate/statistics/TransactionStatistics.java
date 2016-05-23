package com.vladmihalcea.book.hpjp.hibernate.statistics;

import org.hibernate.stat.internal.ConcurrentStatisticsImpl;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <code>TransactionStatistics</code> - Transaction Statistics
 *
 * @author Vlad Mihalcea
 */
public class TransactionStatistics extends ConcurrentStatisticsImpl {

    private static final ThreadLocal<AtomicLong> startNanos = new ThreadLocal<AtomicLong>() {
        @Override protected AtomicLong initialValue() {
            return new AtomicLong();
        }
    };

    private static final ThreadLocal<AtomicLong> connectionCounter = new ThreadLocal<AtomicLong>() {
        @Override protected AtomicLong initialValue() {
            return new AtomicLong();
        }
    };

    private StatisticsReport report = new StatisticsReport();

    @Override public void connect() {
        connectionCounter.get().incrementAndGet();
        startNanos.get().compareAndSet(0, System.nanoTime());
        super.connect();
    }

    @Override public void endTransaction(boolean success) {
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
