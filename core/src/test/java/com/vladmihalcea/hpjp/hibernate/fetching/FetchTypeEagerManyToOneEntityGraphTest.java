package com.vladmihalcea.hpjp.hibernate.fetching;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.jpa.SpecHints;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class FetchTypeEagerManyToOneEntityGraphTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");

            entityManager.persist(post);

            entityManager.persist(
                new PostComment()
                    .setPost(post)
                    .setReview("The first part is about JDBC")
            );

            entityManager.persist(
                new PostComment()
                    .setPost(post)
                    .setReview("The second part is about JPA")
            );

            entityManager.persist(
                new PostComment()
                    .setPost(post)
                    .setReview("The third part is about jOOQ")
            );
        });
    }

    @Test
    public void testFindById() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L);
        });

        assertTrue(Hibernate.isInitialized(comment.getPost()));
        assertEquals(
            "High-Performance Java Persistence",
            comment.getPost().getTitle()
        );
    }

    @Test
    public void testQueryById() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select pc
                from PostComment pc
                where pc.id = :id
                """, PostComment.class)
            .setParameter("id", 1L)
            .getSingleResult();
        });

        assertTrue(Hibernate.isInitialized(comment.getPost()));
        assertEquals(
            "High-Performance Java Persistence",
            comment.getPost().getTitle()
        );
    }

    @Test
    public void testFindByIdFetchGraphOverridesFetchTypeEager() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L,
                Map.of(
                    SpecHints.HINT_SPEC_FETCH_GRAPH,
                    entityManager.createEntityGraph(PostComment.class)
                )
            );
        });
        assertFalse(Hibernate.isInitialized(comment.getPost()));
        try {
            comment.getPost().getTitle();

            fail("Should throw LazyInitializationException");
        } catch(LazyInitializationException expected) {}
    }

    @Test
    public void testQueryByIdFetchGraphOverridesFetchTypeEager() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select pc
                from PostComment pc
                where pc.id = :id
                """, PostComment.class)
            .setHint(
                SpecHints.HINT_SPEC_FETCH_GRAPH,
                entityManager.createEntityGraph(PostComment.class)
            )
            .setParameter("id", 1L)
            .getSingleResult();
        });
        assertFalse(Hibernate.isInitialized(comment.getPost()));
        try {
            comment.getPost().getTitle();

            fail("Should throw LazyInitializationException");
        } catch(LazyInitializationException expected) {}
    }

    @Test
    public void testLoadGraphDoesNotOverrideFetchTypeEager() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L,
                Map.of(
                    SpecHints.HINT_SPEC_LOAD_GRAPH,
                    entityManager.createEntityGraph(PostComment.class)
                )
            );
        });
        assertTrue(Hibernate.isInitialized(comment.getPost()));
        assertEquals(
            "High-Performance Java Persistence",
            comment.getPost().getTitle()
        );
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        @Column(length = 100)
        private String title;

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        @Column(length = 250)
        private String review;

        @ManyToOne
        private Post post;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }
}
