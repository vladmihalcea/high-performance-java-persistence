package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider.PostComment;

public class DriverManagerConnectionProviderTest extends AbstractConnectionProviderTest {

    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();
        properties.put("hibernate.connection.driver_class", "org.hsqldb.jdbc.JDBCDriver");
        properties.put("hibernate.connection.url", dataSourceProvider.url());
        properties.put("hibernate.connection.username", dataSourceProvider.username());
        properties.put("hibernate.connection.password", dataSourceProvider.password());
    }

    @Override
    public Class<? extends ConnectionProvider> expectedConnectionProviderClass() {
        return DriverManagerConnectionProviderImpl.class;
    }

    @Test
    public void testConnection() {
        for (final AtomicLong i = new AtomicLong(); i.get() < 5; i.incrementAndGet()) {
            doInJPA(em -> {
                em.persist(new Post(i.get()));
            });
        }

        doInJPA(em -> {
            Post post = em.find(Post.class, 1L);
            PostComment comment = new PostComment("abc");
            comment.setId(1L);
            post.addComment(comment);
            em.persist(comment);
        });
        doInJPA(em -> {
            em.createQuery("select p from Post p join fetch p.comments", Post.class).getResultList();
        });
    }
}
