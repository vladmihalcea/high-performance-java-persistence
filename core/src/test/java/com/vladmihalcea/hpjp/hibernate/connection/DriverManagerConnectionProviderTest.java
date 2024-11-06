package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider.PostComment;

public class DriverManagerConnectionProviderTest extends AbstractConnectionProviderTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }

    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();

        String url = dataSourceProvider.url();
        String username = dataSourceProvider.username();
        String password = dataSourceProvider.password();

        properties.put("hibernate.connection.driver_class", dataSourceProvider.driverClassName().getName());
        properties.put("hibernate.connection.url", url);
        properties.put("hibernate.connection.username", username);
        properties.put("hibernate.connection.password", password);
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
