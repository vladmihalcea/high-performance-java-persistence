package com.vladmihalcea.hpjp.hibernate.identifier;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import java.util.Properties;

public class StringSequenceIdentifierTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            Board.class,
            Event.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty("entity.identifier.prefix", "ID_");
    }

    @Test
    public void test() {
        executeStatement("DROP SEQUENCE IF EXISTS hibernate_sequence");
        executeStatement("CREATE SEQUENCE hibernate_sequence START 1 INCREMENT 1");

        LOGGER.debug("test");
        doInJPA(entityManager -> {
            entityManager.persist(new Post());
            entityManager.persist(new Post());
            entityManager.persist(new Post());
        });
        doInJPA(entityManager -> {
            entityManager.persist(new Board());
            entityManager.persist(new Board());
            entityManager.persist(new Board());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Identifiable<String> {

        @Id
        @GenericGenerator(
            name = "assigned-sequence",
            strategy = "com.vladmihalcea.hpjp.hibernate.identifier.StringSequenceIdentifier",
            parameters = {
                @org.hibernate.annotations.Parameter(
                    name = "sequence_name", value = "hibernate_sequence"),
                @org.hibernate.annotations.Parameter(
                    name = "sequence_prefix", value = "CTC_"),
                @org.hibernate.annotations.Parameter(
                    name = "increment_size", value = "1"),
            }
        )
        @GeneratedValue(
            generator = "assigned-sequence"
        )
        private String id;

        @Override
        public String getId() {
            return id;
        }
    }

    @Entity(name = "Board")
    public static class Board {

        @Id
        @GenericGenerator(
            name = "assigned-sequence",
            strategy = "com.vladmihalcea.hpjp.hibernate.identifier.StringSequenceIdentifier",
            parameters = {
                @org.hibernate.annotations.Parameter(
                    name = "sequence_name", value = "hibernate_sequence"),
                @org.hibernate.annotations.Parameter(
                    name = "increment_size", value = "1"),
            }
        )
        @GeneratedValue(generator = "assigned-sequence", strategy = GenerationType.SEQUENCE)
        private String id;
    }

    @Entity(name = "Event")
    public static class Event {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private String id;
    }
}
