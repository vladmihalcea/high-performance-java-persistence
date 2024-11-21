package com.vladmihalcea.hpjp.hibernate.logging;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.hpjp.util.logging.InlineQueryLogEntryCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JSlowQueryListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DataSourceProxySlowQueryLogTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "5");
    }

    @Override
    protected boolean connectionPooling() {
        return true;
    }

    @Test
    public void testSlowQueryLog() throws InterruptedException {
        doInJPA(entityManager -> {
            LongStream
                .rangeClosed(1, 50 * 100)
                .forEach(i -> {
                    entityManager.persist(
                        new Post()
                            .setId(i)
                            .setTitle(
                                String.format(
                                    "High-Performance Java Persistence book - page %d",
                                    i
                                )
                            )
                    );
                    if(i % 50 == 0 && i > 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                });
        });

        LOGGER.info("Check slow JPQL query");

        int threadCount = connectionPoolSize();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        Collection<Callable<Void>> callables = IntStream.range(0, threadCount)
            .mapToObj(i -> (Callable<Void>) () -> {
                doInJPA(entityManager -> {
                    List<Post> posts = entityManager.createQuery("""
                        select p
                        from Post p
                        where lower(title) like :titlePattern
                        order by p.id desc
                        """, Post.class)
                    .setParameter("titlePattern", "%Java%book%".toLowerCase())
                    .setFirstResult(4000)
                    .setMaxResults(100)
                    .getResultList();

                    assertEquals(100, posts.size());
                });
                return null;
            })
            .toList();

        long startNanos = System.nanoTime();
        List<Future<Void>> futures = executorService.invokeAll(callables);
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException| ExecutionException e) {
                LOGGER.error(e.getMessage());
            }
        }
        LOGGER.info(
            "{} threads ran in [{}] ms",
            threadCount,
            TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos
        ));
    }

    protected DataSource dataSourceProxy(DataSource dataSource) {
        String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

        SLF4JSlowQueryListener slowQueryListener = new SLF4JSlowQueryListener();
        slowQueryListener.setThreshold(25);
        slowQueryListener.setThresholdTimeUnit(TimeUnit.MILLISECONDS);

        DataSource proxyDataSource = ProxyDataSourceBuilder
            .create(dataSource)
            .name(DATA_SOURCE_PROXY_NAME)
            .listener(slowQueryListener)
            .build();

        return proxyDataSource;
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}
