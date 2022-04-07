package com.vladmihalcea.book.hpjp.hibernate.mapping.generated;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SequenceDefaultColumnValueTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    public void init() {
        executeStatement("DROP SEQUENCE sensor_seq");
        executeStatement("""
            CREATE SEQUENCE
               sensor_seq
            START 100
            """
        );
        super.init();
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("select p from Post p", Post.class).getSingleResult();

            assertEquals(Long.valueOf(100), post.getSequenceId());
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Column(
            columnDefinition = "int8 DEFAULT nextval('sensor_seq')",
            insertable = false
        )
        private Long sequenceId;

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

        public Long getSequenceId() {
            return sequenceId;
        }

        public void setSequenceId(Long sequenceId) {
            this.sequenceId = sequenceId;
        }
    }
}
