package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.providers.MariaDBDataSourceProvider;
import org.hibernate.dialect.MariaDB102Dialect;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Properties;

public class MariaDBIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Override
    protected Database database() {
        return Database.MARIADB;
    }

    /*@Override
    protected DataSourceProvider dataSourceProvider() {
        return new MariaDBDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return MariaDB102Dialect.class.getName();
            }
        };
    }*/

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "10");
        return properties;
    }

    @Test
    @Ignore
    public void testSequenceIdentifierGenerator() {
        doInJPA(entityManager -> {
            for (int i = 0; i < 3; i++) {
                Post post = new Post();
                post.setTitle(
                        String.format("High-Performance Java Persistence, Part %d", i + 1)
                );
                entityManager.persist(post);
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(
            strategy = GenerationType.SEQUENCE
        )
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

}
