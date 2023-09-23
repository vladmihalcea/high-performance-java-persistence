package com.vladmihalcea.hpjp.hibernate.transaction.identifier;

import com.vladmihalcea.hpjp.hibernate.batch.BatchingOptimisticLockingTest;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Vlad Mihalcea
 */
public class OracleTransactionIdTest extends AbstractTest {

    @Override
    protected Database database() {
        return Database.ORACLE;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setTitle("High-Performance Java Persistence")
            );

            LOGGER.info("Current transaction id: {}", transactionId(entityManager));

            executeSync(
                () -> doInJPA(_entityManager -> {
                    _entityManager.persist(
                        new Post()
                            .setTitle("High-Performance SQL")
                    );

                    LOGGER.info("Current transaction id: {}", transactionId(_entityManager));
                })
            );
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Version
        private short version;

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
