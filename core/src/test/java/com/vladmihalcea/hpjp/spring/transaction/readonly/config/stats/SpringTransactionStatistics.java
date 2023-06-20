package com.vladmihalcea.hpjp.spring.transaction.readonly.config.stats;


import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.internal.StatisticsImpl;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Vlad Mihalcea
 */
public class SpringTransactionStatistics extends StatisticsImpl {

    private static final ThreadLocal<SpringTransactionStatisticsReport> reportHolder = new ThreadLocal<>();

    private static final ThreadLocal<AtomicLong> startNanos = ThreadLocal.withInitial(AtomicLong::new);

    private SpringTransactionStatisticsReport report;

    public SpringTransactionStatistics(SessionFactoryImplementor sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public void openSession() {
        super.openSession();
        report = new SpringTransactionStatisticsReport();
        reportHolder.set(report);
    }

    @Override
    public void connect() {
        startNanos.get().compareAndSet(0, System.nanoTime());
        super.connect();
    }

    @Override
    public void endTransaction(boolean success) {
        try {
            report.transactionTime(System.nanoTime() - startNanos.get().get());
            report.generate();
        } finally {
            startNanos.remove();
        }
        super.endTransaction(success);
    }

    public static SpringTransactionStatisticsReport report() {
        return reportHolder.get();
    }
}
