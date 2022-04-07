package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class EntityGraphEagerPostTest extends AbstractPostgreSQLIntegrationTest {

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
            long postId = 1;
            long commentId = 1L;
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(postId++)
                        .setTitle(String.format("High-Performance Java Persistence, part %d", i))
                        .addComment(
                            new PostComment()
                                .setId(commentId++)
                                .setReview("The first part is about JDBC")
                        )
                        .addComment(
                            new PostComment()
                                .setId(commentId++)
                                .setReview("The second part is about JPA")
                        )
                        .addComment(
                            new PostComment()
                                .setId(commentId++)
                                .setReview("The third part is about jOOQ")
                        )
                );
            }
        });
    }

    @Test
    public void testFind() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);

            LOGGER.info("The comment post title is '{}'", comment.getPost().getTitle());
        });
    }

    @Test
    public void testJPQL() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.createQuery("""
                select pc
                from PostComment pc
                left join fetch pc.post
                where pc.id = :id
                """, PostComment.class)
            .setParameter("id", 1L)
            .getSingleResult();

            LOGGER.info("The comment post title is '{}'", comment.getPost().getTitle());
        });
    }

    @Test
    public void testFindWithProgrammaticEntityGraph() {
        doInJPA(entityManager -> {
            EntityGraph<PostComment> postCommentGraph = entityManager.createEntityGraph(PostComment.class);
            postCommentGraph.addAttributeNodes("post");

            PostComment comment = entityManager.find(
                PostComment.class,
                1L,
                Collections.singletonMap(
                    "jakarta.persistence.loadgraph",
                    postCommentGraph
                )
            );

            LOGGER.info("The comment post title is '{}'", comment.getPost().getTitle());
        });
    }

    @Test
    public void testFindWithDeclaredEntityGraph() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(
                PostComment.class,
                1L,
                Collections.singletonMap(
                    "jakarta.persistence.loadgraph",
                    entityManager.getEntityGraph("PostComment.post")
                )
            );
        });
        assertNotNull(comment.getPost());
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
    @NamedEntityGraph(
        name = "PostComment.post",
        attributeNodes = @NamedAttributeNode("post")
    )
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
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
