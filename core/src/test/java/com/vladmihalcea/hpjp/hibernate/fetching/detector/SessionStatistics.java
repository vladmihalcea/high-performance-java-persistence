package com.vladmihalcea.hpjp.hibernate.fetching.detector;

import com.vladmihalcea.hpjp.util.ReflectionUtils;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.internal.StatisticsImpl;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.stat.spi.StatisticsImplementor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author Vlad Mihalcea
 */
public class SessionStatistics extends StatisticsImpl {

    private static final ThreadLocal<Map<Class, AtomicInteger>> entityFetchCountContext = new ThreadLocal<>();

    public SessionStatistics(SessionFactoryImplementor sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public void openSession() {
        entityFetchCountContext.set(new LinkedHashMap<>());
        super.openSession();
    }

    @Override
    public void fetchEntity(String entityName) {
        Map<Class, AtomicInteger> entityFetchCountMap = entityFetchCountContext.get();
        entityFetchCountMap.computeIfAbsent(ReflectionUtils.getClass(entityName), clazz -> new AtomicInteger()).incrementAndGet();
        super.fetchEntity(entityName);
    }

    @Override
    public void closeSession() {
        entityFetchCountContext.remove();
        super.closeSession();
    }

    public static int getEntityFetchCount(String entityClassName) {
        return getEntityFetchCount(
            ReflectionUtils.getClass(entityClassName)
        );
    }

    public static int getEntityFetchCount(Class entityClass) {
        AtomicInteger entityFetchCount = entityFetchCountContext.get().get(entityClass);
        return entityFetchCount != null ? entityFetchCount.get() : 0;
    }

    public static class Factory implements StatisticsFactory {

        public static final Factory INSTANCE = new Factory();

        @Override
        public StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory) {
            return new SessionStatistics(sessionFactory);
        }
    }
}
