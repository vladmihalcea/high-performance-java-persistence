package com.vladmihalcea.hpjp.hibernate.logging;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import com.vladmihalcea.hpjp.hibernate.statistics.SlowQueryLogTest;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.hpjp.util.logging.InlineQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JSlowQueryListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DataSourceProxyTest extends AbstractTest {

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

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("Post it!");

            entityManager.persist(post);
        });
    }

    @Test
    public void testBatch() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Post no. %d", i))
                );
            }
        });
    }

    @Test
    public void testSlowQueryLog() {
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
        sleep(100);
    }

    protected DataSource dataSourceProxy(DataSource dataSource) {
        String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

        SLF4JSlowQueryListener slowQueryListener = new SLF4JSlowQueryListener();
        slowQueryListener.setThreshold(25);
        slowQueryListener.setThresholdTimeUnit(TimeUnit.MILLISECONDS);

        ChainListener listener = new ChainListener();
        listener.addListener(slowQueryListener);
        SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
        loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
        listener.addListener(loggingListener);

        DataSource proxyDataSource = ProxyDataSourceBuilder
            .create(dataSource)
            .name(DATA_SOURCE_PROXY_NAME)
            .listener(listener)
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
