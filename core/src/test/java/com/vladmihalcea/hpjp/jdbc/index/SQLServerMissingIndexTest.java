package com.vladmihalcea.hpjp.jdbc.index;

import com.vladmihalcea.hpjp.jdbc.index.providers.IndexEntityProvider;
import com.vladmihalcea.hpjp.jdbc.index.providers.IndexEntityProvider.Task;
import com.vladmihalcea.hpjp.util.AbstractSQLServerIntegrationTest;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerMissingIndexTest extends AbstractSQLServerIntegrationTest {

    private final IndexEntityProvider entityProvider = new IndexEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "500");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    @Test
    public void testInsert() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        long startNanos = System.nanoTime();
        doInJPA(entityManager -> {
            int taskCount = getPostCount();

            for (int i = 1; i <= taskCount; i++) {
                Task.Status status = Task.Status.DONE;
                if (i >= 99000) {
                    status = Task.Status.TO_DO;
                } else if (i >= 95000) {
                    status = Task.Status.FAILED;
                }
                entityManager.persist(
                    new Task().setStatus(status)
                );
            }
        });
        LOGGER.info("{}.testInsert took {} millis",
                getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        for (int i = 1; i <= 100; i++) {
            doInJPA(entityManager -> {
                List<Task> tasks = entityManager.createQuery("""
                select t
                from task t
                where t.status =:status
                """, Task.class)
            .setParameter("status", Task.Status.random())
            .getResultList();

            });
        }

        LOGGER.info("Check missing indexes");
        //TODO: Not done
    }

    protected int getPostCount() {
        return 100 * 1000;
    }

    protected int getBatchSize() {
        return 100;
    }
}
