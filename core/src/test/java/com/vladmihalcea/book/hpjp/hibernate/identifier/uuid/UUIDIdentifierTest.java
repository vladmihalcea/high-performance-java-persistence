package com.vladmihalcea.book.hpjp.hibernate.identifier.uuid;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

import static org.junit.Assert.assertSame;

public class UUIDIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testUUIDIdentifierGenerator() {
        LOGGER.debug("testUUIDIdentifierGenerator");
        doInJPA(entityManager -> {
            entityManager.persist(new Post());
            entityManager.flush();
            entityManager.merge(new Post());
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue(generator = "uuid")
        @GenericGenerator(name = "uuid", strategy = "uuid")
        @Column(columnDefinition = "CHAR(32)")
        private String id;
    }
}
