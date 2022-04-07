package com.vladmihalcea.book.hpjp.hibernate.concurrency.version;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class DefaultMinValueVersionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Test
    public void testOptimisticLocking() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            entityManager.flush();

            int updateCount = entityManager.createNativeQuery(
                "UPDATE post SET version = :version WHERE id = :id")
            .setParameter("version", Short.MAX_VALUE)
            .setParameter("id", 1L)
            .executeUpdate();

            assertSame(1, updateCount);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(Short.MAX_VALUE, post.getVersion());

            post.setTitle("High-Performance Java Persistence, 2nd edition");
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(Short.MIN_VALUE, post.getVersion());

            post.setTitle("High-Performance Java Persistence, 3nd edition");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private Short version;

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

        public int getVersion() {
            return version;
        }
    }

}
