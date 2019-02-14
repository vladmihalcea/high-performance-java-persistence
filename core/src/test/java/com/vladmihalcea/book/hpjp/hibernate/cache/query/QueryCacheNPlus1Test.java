package com.vladmihalcea.book.hpjp.hibernate.cache.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.jpa.QueryHints;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class QueryCacheNPlus1Test extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                TestEntity.class,
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "ehcache");
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            entityManager.persist(new TestEntity("1"));
            entityManager.persist(new TestEntity("2"));
            entityManager.persist(new TestEntity("3"));
            entityManager.persist(new TestEntity("4"));
            entityManager.persist(new TestEntity("5"));
        });
    }

    @After
    public void destroy() {
        entityManagerFactory().getCache().evictAll();
        super.destroy();
    }

    public List<TestEntity> getEntities(
            EntityManager entityManager) {
        return entityManager.createQuery(
                "from TestEntity", TestEntity.class)
                .setHint(QueryHints.HINT_CACHEABLE, true)
                .getResultList();
    }

    @Test
    public void test() {
        LOGGER.info("first call, cache should be empty here");
        doInJPA(entityManager -> {
            assertEquals(5, getEntities(entityManager).size());
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }

        LOGGER.info("second call, data should be taken from the cache");
        doInJPA(entityManager -> {
            assertEquals(5, getEntities(entityManager).size());
        });

        try {
            Thread.sleep(12000);
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }

        LOGGER.info("third call, cache should be empty here");
        doInJPA(entityManager -> {
            assertEquals(5, getEntities(entityManager).size());
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }

        LOGGER.info("fourth call, data should be taken from the cache but they are not");
        doInJPA(entityManager -> {
            assertEquals(5, getEntities(entityManager).size());
        });
    }
}
