package com.vladmihalcea.book.hpjp.hibernate.statistics;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ConnectionStatisticsTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return BlogEntityProvider.INSTANCE.entities();
    }

    @Test
    public void testJdbcOneToManyMapping() {
        int iterations = 5;

        for (long i = 0; i < iterations; i++) {
            final Long id = i + 1;
            doInJPA(entityManager -> {
                Post post = new Post();
                post.setId(id);
                post.setTitle(
                    String.format(
                        "High-Performance Java Persistence, Part %d", id
                    )
                );
                entityManager.persist(post);

                Number postCount = entityManager.createQuery(
                    "select count(*) from Post", Number.class)
                .getSingleResult();

                assertEquals((long) id, postCount.longValue());
            });
        }
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put(AvailableSettings.GENERATE_STATISTICS, "true");
        properties.put("hibernate.stats.factory", TransactionStatisticsFactory.class.getName());
        return properties;
    }
}
