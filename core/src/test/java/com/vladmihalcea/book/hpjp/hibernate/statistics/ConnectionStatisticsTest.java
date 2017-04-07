package com.vladmihalcea.book.hpjp.hibernate.statistics;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;

/**
 * ConnectionStatisticsTest - Test Hibernate statistics
 *
 * @author Vlad Mihalcea
 */
public class ConnectionStatisticsTest extends AbstractTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testJdbcOneToManyMapping() {
        doInJPA(connection -> {

        });
        doInJPA(connection -> {

        });
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put(AvailableSettings.GENERATE_STATISTICS, "true");
        properties.put("hibernate.stats.factory",
            TransactionStatisticsFactory.class.getName());
        return properties;
    }
}
