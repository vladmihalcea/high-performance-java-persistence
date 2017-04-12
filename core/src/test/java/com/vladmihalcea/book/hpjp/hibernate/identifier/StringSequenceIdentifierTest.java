package com.vladmihalcea.book.hpjp.hibernate.identifier;

import java.util.Properties;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;

public class StringSequenceIdentifierTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Board.class,
            Event.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.setProperty( "entity.identifier.prefix", "ID_" );
        return properties;
    }

    @Test
    public void test() {
        LOGGER.debug("test");
        doInJPA(entityManager -> {
            entityManager.persist(new Post());
            entityManager.persist(new Post("ABC"));
            entityManager.persist(new Post());
            entityManager.persist(new Post("DEF"));
            entityManager.persist(new Post());
            entityManager.persist(new Post());
        });
        doInJPA(entityManager -> {
            entityManager.persist(new Board());
            entityManager.persist(new Board("ABC"));
            entityManager.persist(new Board());
            entityManager.persist(new Board("DEF"));
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
            strategy = "com.vladmihalcea.book.hpjp.hibernate.identifier.StringSequenceIdentifier",
            parameters = {
                @org.hibernate.annotations.Parameter(
                        name = "sequence_name", value = "hibernate_sequence"),
                @org.hibernate.annotations.Parameter(
                        name = "sequence_prefix", value = "CTC_"),
            }
        )
        @GeneratedValue(generator = "assigned-sequence", strategy = GenerationType.SEQUENCE)
        private String id;

        @Version
        private Integer version;

        public Post() {
        }

        public Post(String id) {
            this.id = id;
        }

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
                strategy = "com.vladmihalcea.book.hpjp.hibernate.identifier.StringSequenceIdentifier",
                parameters = {
                        @org.hibernate.annotations.Parameter(
                                name = "sequence_name", value = "hibernate_sequence"),
                }
        )
        @GeneratedValue(generator = "assigned-sequence", strategy = GenerationType.SEQUENCE)
        private String id;

        @Version
        private Integer version;

        public Board() {
        }

        public Board(String id) {
            this.id = id;
        }
    }

    @Entity(name = "Event")
    public static class Event {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private String id;
    }

}
