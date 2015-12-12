package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

public class HiloIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testHiloIdentifierGenerator() {
        doInJPA(entityManager -> {
            for(int i = 0; i < 4; i++) {
                Post post = new Post();
                entityManager.persist(post);
            }
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hilo")
        @GenericGenerator(
            name = "hilo",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {
                @Parameter(name = "sequence_name", value = "sequence"),
                @Parameter(name = "initial_value", value = "1"),
                @Parameter(name = "increment_size", value = "3"),
                @Parameter(name = "optimizer", value = "hilo")
            }
        )
        private Long id;
    }


}
