package com.vladmihalcea.book.hpjp.hibernate.transaction.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class TransactionIdTest extends AbstractTest {

    private Database database;

    public TransactionIdTest(Database database) {
        this.database = database;
    }

    @Override
    protected Database database() {
        return database;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Parameterized.Parameters
    public static Collection<Object[]> databases() {
        return Arrays.asList(
                new Object[]{Database.HSQLDB},
                new Object[]{Database.ORACLE},
                new Object[]{Database.SQLSERVER},
                new Object[]{Database.POSTGRESQL},
                new Object[]{Database.MYSQL}
        );
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            LOGGER.info("Current transaction id: {}", transactionId(entityManager));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

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
