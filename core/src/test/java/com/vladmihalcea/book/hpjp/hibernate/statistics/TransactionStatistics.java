package com.vladmihalcea.book.hpjp.hibernate.statistics;

import org.hibernate.stat.internal.ConcurrentStatisticsImpl;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <code>TransactionStatistics</code> - Transaction Statistics
 *
 * @author Vlad Mihalcea
 */
public class TransactionStatistics extends ConcurrentStatisticsImpl {

    private static final ThreadLocal<AtomicLong> transactionStartNanos = new ThreadLocal<AtomicLong>() {
        @Override
        protected AtomicLong initialValue() {
            return new AtomicLong();
        }
    };

    private static final ThreadLocal<AtomicLong> connectionCounter = new ThreadLocal<AtomicLong>() {
        @Override
        protected AtomicLong initialValue() {
            return new AtomicLong();
        }
    };

    private StatisticsReport report = new StatisticsReport();

    @Override public void connect() {
        connectionCounter.get().incrementAndGet();
        transactionStartNanos.get().compareAndSet(System.nanoTime(), 0);
        super.connect();
    }

    @Override public void endTransaction(boolean success) {
        report.transactionTime(System.nanoTime() - transactionStartNanos.get().longValue());
        report.connectionsCount(connectionCounter.get().longValue());
        report.generate();
        transactionStartNanos.remove();
        connectionCounter.remove();
        super.endTransaction(success);
    }
}
