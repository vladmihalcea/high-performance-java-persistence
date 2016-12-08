package com.vladmihalcea.book.hpjp.hibernate.cache.readwrite;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * ReadWriteCacheConcurrencyStrategyWithTimeoutTest - Test to check CacheConcurrencyStrategy.READ_WRITE with lock timeout
 *
 * @author Vlad Mihalcea
 */
public class ReadWriteCacheConcurrencyStrategyWithLockTimeoutTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Repository.class
        };
    }

    private AtomicBoolean applyInterceptor = new AtomicBoolean();

    @Override
    protected Interceptor interceptor() {
        return new EmptyInterceptor() {
            @Override
            public void beforeTransactionCompletion(Transaction tx) {
                if(applyInterceptor.get()) {
                    tx.rollback();
                }
            }
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        properties.put("net.sf.ehcache.hibernate.cache_lock_timeout", String.valueOf(250));
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Repository repository = new Repository("Hibernate-Master-Class");
            entityManager.persist(repository);
        });
    }

    @Test
    public void testRepositoryEntityUpdate() {
        try {
            doInJPA(entityManager -> {
                Repository repository = (Repository) entityManager.find(Repository.class, 1L);
                repository.setName("High-Performance Hibernate");
                applyInterceptor.set(true);
            });
        } catch (Exception e) {
            LOGGER.info("Expected", e);
        }
        applyInterceptor.set(false);

        AtomicReference<Object> previousCacheEntryReference = new AtomicReference<>();
        AtomicBoolean cacheEntryChanged = new AtomicBoolean();

        while (!cacheEntryChanged.get()) {
            doInJPA(entityManager -> {
                boolean entryChange;
                entityManager.find(Repository.class, 1L);
                try {
                    Object previousCacheEntry = previousCacheEntryReference.get();
                    Object cacheEntry = getCacheEntry(Repository.class, 1L);
                    entryChange = previousCacheEntry != null &&
                        previousCacheEntry != cacheEntry;
                    previousCacheEntryReference.set(cacheEntry);
                    LOGGER.info("Cache entry {}", ToStringBuilder.reflectionToString(cacheEntry));
                    if(!entryChange) {
                        sleep(100);
                    } else {
                        cacheEntryChanged.set(true);
                    }
                } catch (IllegalAccessException e) {
                    LOGGER.error("Error accessing Cache", e);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getCacheEntry(Class<T> clazz, Long id) throws IllegalAccessException {
        EntityPersister entityPersister = ((SessionFactoryImplementor) sessionFactory()).getEntityPersister(clazz.getName() );
        return (T) getCache(clazz).get(cacheKey(1L, entityPersister));
    }

    private net.sf.ehcache.Cache getCache(Class clazz) throws IllegalAccessException {
        EntityPersister entityPersister = ((SessionFactoryImplementor) sessionFactory()).getEntityPersister(clazz.getName() );
        EntityRegion region = entityPersister.getCacheAccessStrategy().getRegion();
        Field cacheField = getField(region.getClass(), "cache");
        return  (net.sf.ehcache.Cache) cacheField.get(region);
    }

    private Field getField(Class clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            if(clazz.getSuperclass() != null && clazz.getSuperclass() != clazz) {
                return getField(clazz.getSuperclass(), fieldName);
            }
        }
        throw new IllegalArgumentException("No field " + fieldName + " found!");
    }

    private Object cacheKey(Serializable identifier, EntityPersister p) {
        return DefaultCacheKeysFactory.INSTANCE.createEntityKey(
                identifier, p, (SessionFactoryImplementor) sessionFactory(), null
        );
    }

    /**
     * Repository - Repository
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "repository")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Repository {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        @Version
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
    }
}
