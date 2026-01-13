package com.vladmihalcea.hpjp.hibernate.flushing;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;
import org.junit.jupiter.api.Test;

import java.util.List;

import org.hibernate.jpa.HibernateHints.*;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Vlad Mihalcea
 */
public class ReadOnlyQueryTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);
        });
    }

    @Test
    public void testReadOnly() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                """, Post.class)
            .setHint(HibernateHints.HINT_READ_ONLY, true)
            .getResultList();
        });
    }

    @Test
    public void testDefaultReadOnly() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            boolean isDefaultReadOnly = session.isDefaultReadOnly();
            assertFalse(isDefaultReadOnly);
            session.setDefaultReadOnly(true);
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                """, Post.class)
            .getResultList();
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

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
    }
}
