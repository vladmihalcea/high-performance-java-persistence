package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class HibernateInitializeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.cache.region.factory_class", "jcache");
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post()
            .setId(1L)
            .setTitle("High-Performance Java Persistence");

            post.addComment(
                new PostComment()
                .setId(1L)
                .setReview("A must-read!")
            );

            post.addComment(
                new PostComment()
                .setId(2L)
                .setReview("Awesome!")
            );

            post.addComment(
                new PostComment()
                .setId(3L)
                .setReview("5 stars")
            );

            entityManager.persist(post);
        });
    }

    @Test
    public void testEntityProxyWithoutSecondLevelCache() {

        doInJPA(entityManager -> {
            LOGGER.info("Clear the second-level cache");

            entityManager.getEntityManagerFactory().getCache().evictAll();

            LOGGER.info("Loading a PostComment");

            PostComment comment = entityManager.find(
                PostComment.class,
                1L
            );

            assertEquals(
                    "A must-read!",
                    comment.getReview()
            );

            Post post = comment.getPost();

            LOGGER.info("Post entity class: {}", post.getClass().getName());

            Hibernate.initialize(post);

            assertEquals(
                    "High-Performance Java Persistence",
                    post.getTitle()
            );

            Hibernate.initialize(post);
        });
    }

    @Test
    public void testEntityProxyJoinFetchWithoutSecondLevelCache() {

        doInJPA(entityManager -> {
            LOGGER.info("Clear the second-level cache");

            entityManager.getEntityManagerFactory().getCache().evictAll();

            LOGGER.info("Loading a PostComment");

            PostComment comment = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "join fetch pc.post " +
                "where pc.id = :id", PostComment.class)
            .setParameter("id", 1L)
            .getSingleResult();

            assertEquals(
                    "A must-read!",
                    comment.getReview()
            );

            Post post = comment.getPost();

            LOGGER.info("Post entity class: {}", post.getClass().getName());

            assertEquals(
                    "High-Performance Java Persistence",
                    post.getTitle()
            );
        });
    }

    @Test
    public void testEntityProxy() {

        doInJPA(entityManager -> {
            LOGGER.info("Loading a PostComment");

            PostComment comment = entityManager.find(
                PostComment.class,
                1L
            );

            assertEquals(
                    "A must-read!",
                    comment.getReview()
            );

            Post post = comment.getPost();

            LOGGER.info("Proxy class: {}", post.getClass().getName());

            Hibernate.initialize(post);

            assertEquals(
                    "High-Performance Java Persistence",
                    post.getTitle()
            );

            Hibernate.initialize(post);
        });
    }

    @Test
    public void testCollectionProxy() {

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals(3, post.getComments().size());
        });

        doInJPA(entityManager -> {
            LOGGER.info("Loading a Post");

            Post post = entityManager.find(
                    Post.class,
                    1L
            );

            List<PostComment> comments = post.getComments();

            LOGGER.info("Collection class: {}", comments.getClass().getName());

            Hibernate.initialize(comments);

            LOGGER.info("Post comments: {}", comments);
        });
    }

    @Test
    public void testDetachedProxy() {
        Post post = doInJPA(entityManager -> {
            LOGGER.info("Loading a Post");

            return entityManager.find(
                    Post.class,
                    1L
            );
        });

        doInJPA(entityManager -> {

            entityManager.unwrap(Session.class).update(post);

            List<PostComment> comments = post.getComments();

            LOGGER.info("Collection class: {}", comments.getClass().getName());

            Hibernate.initialize(comments);

            LOGGER.info("Post comments: {}", comments);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
        @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
        private List<PostComment> comments = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public List<PostComment> getComments() {
            return comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Post)) return false;
            return id != null && id.equals(((Post) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class PostComment {

        @Id
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostComment)) return false;
            return id != null && id.equals(((PostComment) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            return "PostComment{" +
                    "id=" + id +
                    ", review='" + review + '\'' +
                    '}';
        }
    }
}
