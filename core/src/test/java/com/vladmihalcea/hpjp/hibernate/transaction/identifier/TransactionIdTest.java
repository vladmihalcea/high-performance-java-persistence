package com.vladmihalcea.hpjp.hibernate.transaction.identifier;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runners.Parameterized;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.runner.RunWith;

/**
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public class TransactionIdTest extends AbstractTest {

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

    @Parameter
    private Database database;

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(Database.ORACLE),
            Arguments.of(Database.SQLSERVER),
            Arguments.of(Database.POSTGRESQL),
            Arguments.of(Database.MYSQL)
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
        private short version;

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
