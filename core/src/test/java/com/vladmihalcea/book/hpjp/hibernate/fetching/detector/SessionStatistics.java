package com.vladmihalcea.book.hpjp.hibernate.fetching.detector;

import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.stat.internal.StatisticsImpl;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.stat.spi.StatisticsImplementor;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author Vlad Mihalcea
 */
public class SessionStatistics extends StatisticsImpl {

    private static final ThreadLocal<Map<Class, AtomicInteger>> fetchesCountMapContext = new ThreadLocal<>();

    public SessionStatistics(SessionFactoryImplementor sessionFactory) {
        super(sessionFactory);
    }

    public static int getFetchesCount(String entityClassName) {
        AtomicInteger fetchesCount = fetchesCountMapContext.get().get(ReflectionUtils.getClass(entityClassName));
        return fetchesCount != null ? fetchesCount.get() : 0;
    }

    @Override
    public void openSession() {
        fetchesCountMapContext.set(new LinkedHashMap<>());
        super.openSession();
    }

    @Override
    public void fetchEntity(String entityName) {
        Map<Class, AtomicInteger> fetchesCountMap = fetchesCountMapContext.get();
        fetchesCountMap.computeIfAbsent(ReflectionUtils.getClass(entityName), clazz -> new AtomicInteger()).incrementAndGet();
        super.fetchEntity(entityName);
    }

    @Override
    public void closeSession() {
        fetchesCountMapContext.remove();
        super.closeSession();
    }

    public static class SessionStatisticsFactory implements StatisticsFactory {

        public static final SessionStatisticsFactory INSTANCE = new SessionStatisticsFactory();

        @Override
        public StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory) {
            return new SessionStatistics(sessionFactory);
        }
    }
}
