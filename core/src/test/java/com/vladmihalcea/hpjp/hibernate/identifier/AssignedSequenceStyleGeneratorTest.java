package com.vladmihalcea.hpjp.hibernate.identifier;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

public class AssignedSequenceStyleGeneratorTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Test
    public void test() {
        LOGGER.debug("test");
        doInJPA(entityManager -> {
            entityManager.persist(new Post());
            entityManager.merge(new Post().setId(-1L));
            entityManager.persist(new Post());
            entityManager.merge(new Post().setId(-2L));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Identifiable<Long> {

        @Id
        @GenericGenerator(
            name = "assigned-sequence",
            strategy = "com.vladmihalcea.hpjp.hibernate.identifier.AssignedSequenceStyleGenerator",
            parameters = @org.hibernate.annotations.Parameter(
                name = "sequence_name",
                value = "post_sequence"
            )
        )
        @GeneratedValue(generator = "assigned-sequence", strategy = GenerationType.SEQUENCE)
        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }
    }

}
