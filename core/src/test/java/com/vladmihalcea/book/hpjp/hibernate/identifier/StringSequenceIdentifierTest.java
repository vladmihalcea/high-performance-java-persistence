package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;

public class StringSequenceIdentifierTest extends AbstractOracleXEIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Board.class
        };
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
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;
    }

}
