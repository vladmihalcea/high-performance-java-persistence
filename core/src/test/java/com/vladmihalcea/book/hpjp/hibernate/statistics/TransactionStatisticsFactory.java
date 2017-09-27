package com.vladmihalcea.book.hpjp.hibernate.statistics;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * @author Vlad Mihalcea
 */
public class TransactionStatisticsFactory implements StatisticsFactory {

    @Override
    public StatisticsImplementor buildStatistics(
            SessionFactoryImplementor sessionFactory) {
        return new TransactionStatistics();
    }
}
