package com.vladmihalcea.hpjp.hibernate.fetching;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .addComment(
                        new PostComment()
                            .setReview("The first part is about JDBC")
                    )
                    .addComment(
                        new PostComment()
                            .setReview("The second part is about JPA")
                    )
                    .addComment(
                        new PostComment()
                            .setReview("The third part is about jOOQ")
                    )
            );
        });
    }

    @Test
    public void testFetchGraphOverridesFetchTypeEager() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L,
                Collections.singletonMap(
                    "jakarta.persistence.fetchgraph",
                    entityManager.createEntityGraph(PostComment.class)
                )
            );
        });
        assertFalse(Hibernate.isInitialized(comment.getPost()));
        try {
            comment.getPost().getTitle();

            fail("Should have thrown LazyInitializationException");
        } catch(LazyInitializationException expected) {}
    }

    @Test
    public void testLoadGraphDoesNotOverrideFetchTypeEager() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L,
                Collections.singletonMap(
                    "jakarta.persistence.loadgraph",
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

        private String title;

        @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
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

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

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
