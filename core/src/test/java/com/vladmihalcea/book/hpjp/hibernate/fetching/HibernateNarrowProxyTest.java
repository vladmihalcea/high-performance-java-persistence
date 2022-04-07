package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Test;

import jakarta.persistence.*;

import java.util.Properties;

import static org.junit.Assert.*;

public class HibernateNarrowProxyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    private Post _post;

    @Override
    protected void afterInit() {
        _post = doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);
            return post;
        });

        doInJPA(entityManager -> {
            LOGGER.info("Saving a PostComment");

            Post post = entityManager.getReference(Post.class, 1L);

            PostComment comment = new PostComment();
            comment.setId(1L);
            comment.setPost(post);
            comment.setReview("A must-read!");
            entityManager.persist(comment);
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            LOGGER.info("Loading a PostComment");

            PostComment comment = entityManager.find(
                PostComment.class,
                1L
            );

            LOGGER.info("Loading the Post Proxy");

            assertEquals(
                "High-Performance Java Persistence",
                ((Post) comment.getPost()).getTitle()
            );
        });

        doInJPA(entityManager -> {
            LOGGER.info("Equality check");
            Post post = entityManager.getReference(Post.class, 1L);
            LOGGER.info("Post entity class: {}", post.getClass().getName());

            assertFalse(_post.equals(post));

            assertTrue(_post.equals(Hibernate.unproxy(post)));
        });
    }

    @Override
    protected void additionalProperties(Properties properties) {
        System.setProperty("hibernate.bytecode.provider", "javassist");
        properties.put("hibernate.bytecode.allow_enhancement_as_proxy", "false");
    }

    @Test
    public void testNarrowProxy() {
        doInJPA(entityManager -> {
            LOGGER.info("Loading a PostComment");

            PostComment comment = entityManager.find(PostComment.class, 1L);
            Identifiable post = comment.getPost();

            assertTrue((post instanceof HibernateProxy) && !Hibernate.isInitialized(post));
            Hibernate.initialize(post);
            assertTrue(Hibernate.isInitialized(post));

            Post postEntity = entityManager.getReference(Post.class, post.getId());
            assertTrue(Hibernate.isInitialized(postEntity));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Identifiable<Long> {

        @Id
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Post)) return false;
            //Intentionally uses field to prove how Proxy works
            return id != null && id.equals(((Post) o).id);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    public static interface Identifiable<T> {
        T getId();
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment implements Identifiable<Long> {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, targetEntity = Post.class)
        private Identifiable post;

        private String review;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Identifiable getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
