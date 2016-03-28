package com.vladmihalcea.book.hpjp.hibernate.identifier.uuid;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class AssignedUUIDIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testAssignedIdentifierGenerator() {
        LOGGER.debug("testAssignedIdentifierGenerator");
        doInJPA(entityManager -> {
            Post post = new Post();
            LOGGER.debug("persist Post");
            entityManager.persist(post);
            entityManager.flush();
            assertSame(post, entityManager
                .createQuery("select p from Post p where p.id = :uuid", Post.class)
                .setParameter("uuid", post.id)
                .getSingleResult());
            byte[] uuid = (byte[]) entityManager.createNativeQuery("select id from Post").getSingleResult();
            assertNotNull(uuid);
            LOGGER.debug("merge Post");
            entityManager.merge(new Post());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @Column(columnDefinition = "BINARY(16)")
        private UUID id = UUID.randomUUID();

        public Post() {}
    }
}
