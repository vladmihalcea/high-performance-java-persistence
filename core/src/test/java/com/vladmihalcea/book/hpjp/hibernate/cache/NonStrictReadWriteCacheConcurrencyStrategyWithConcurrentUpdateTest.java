package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * NonStrictReadWriteCacheConcurrencyStrategyWithConcurrentUpdateTest - Test to check CacheConcurrencyStrategy.NONSTRICT_READ_WRITE
 *
 * @author Vlad Mihalcea
 */
public class NonStrictReadWriteCacheConcurrencyStrategyWithConcurrentUpdateTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Repository.class
        };
    }

    private AtomicBoolean applyInterceptor = new AtomicBoolean();

    private final CountDownLatch endLatch = new CountDownLatch(1);

    private class BobTransaction extends EmptyInterceptor {
        @Override
        public void beforeTransactionCompletion(Transaction tx) {
            if(applyInterceptor.get()) {
                LOGGER.info("Fetch Repository from another transaction");
                assertFalse(sessionFactory().getCache()
                    .containsEntity(Repository.class, 1L));
                executeSync(() -> {
                    Session _session = sessionFactory().openSession();
                    Repository repository = (Repository)
                        _session.get(Repository.class, 1L);
                    LOGGER.info("Cached Repository from Bob's transaction {}",
                        repository);
                    _session.close();
                    endLatch.countDown();
                });
                assertTrue(sessionFactory().getCache()
                    .containsEntity(Repository.class, 1L));
            }
        }
    }

    @Override
    protected Interceptor interceptor() {
        return new BobTransaction();
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInHibernate(session -> {
            Repository repository = new Repository("Hibernate-Master-Class");
            session.persist(repository);
        });
    }

    @Test
    public void testRepositoryEntityUpdate() throws InterruptedException {
        doInHibernate(session -> {
            LOGGER.info("Load and modify Repository");
            Repository repository = (Repository)
                session.get(Repository.class, 1L);
            assertTrue(sessionFactory().getCache()
                .containsEntity(Repository.class, 1L));
            repository.setName("High-Performance Hibernate");
            applyInterceptor.set(true);
        });
        endLatch.await();
        assertFalse(sessionFactory().getCache()
            .containsEntity(Repository.class, 1L));
        doInHibernate(session -> {
            applyInterceptor.set(false);
            Repository repository = (Repository)
                session.get(Repository.class, 1L);
            LOGGER.info("Cached Repository {}", repository);
        });
    }

    @Test
    public void testOptimisticLocking() {
        LOGGER.info("testOptimisticLocking");
        doInHibernate(session -> {
            LOGGER.info("Load Repository");
            Repository repository = session.get(Repository.class, 1L);
            session.buildLockRequest(new LockOptions().setLockMode(LockMode.OPTIMISTIC)).lock(repository);
        });
        doInHibernate(session -> {
            LOGGER.info("Load Repository again");
            Repository repository = session.get(Repository.class, 1L);
        });
    }

    /**
     * Repository - Repository
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "repository")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    public static class Repository {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;

        @javax.persistence.Version
        private int version;

        public Repository() {
        }

        public Repository(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Repository{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
