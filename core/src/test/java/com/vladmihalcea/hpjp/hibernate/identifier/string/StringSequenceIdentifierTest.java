package com.vladmihalcea.hpjp.hibernate.identifier.string;

import com.vladmihalcea.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

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
        @StringSequence(
            sequenceName = "hibernate_sequence",
            sequencePrefix = "CTC_"
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
        @StringSequence(
            sequenceName = "hibernate_sequence",
            sequencePrefix = "CTC_"
        )
        private String id;
    }

    @Entity(name = "Event")
    public static class Event {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private String id;
    }

}
