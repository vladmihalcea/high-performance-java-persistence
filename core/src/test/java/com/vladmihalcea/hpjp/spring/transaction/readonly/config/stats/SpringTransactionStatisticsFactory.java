package com.vladmihalcea.hpjp.spring.transaction.readonly.config.stats;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * @author Vlad Mihalcea
 */
public class SpringTransactionStatisticsFactory implements StatisticsFactory {

    @Override
    public StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory) {
        return new SpringTransactionStatistics(sessionFactory);
    }
}
