package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class EagerFetchingManyToOneEntityGraphTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    public void afterInit() {
        String[] reviews = new String[] {
            "amazing",
            "awesome",
            "excellent"
        };

        doInJPA(entityManager -> {
            long pastId = 1;
            long commentId = 1;

            for (long i = 1; i <= 3; i++) {
                Post post = new Post()
                    .setId(pastId++)
                    .setTitle(String.format("High-Performance Java Persistence, part %d", i)
                );
                entityManager.persist(post);

                for (int j = 0; j < 3; j++) {
                    entityManager.persist(
                        new PostComment()
                        .setId(commentId++)
                        .setPost(post)
                        .setReview(String.format("The part %d was %s", i, reviews[j]))
                    );
                }
            }

        });
    }

    @Test
    public void testFindWithNamedEntityFetchGraph() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L,
                Collections.singletonMap(
                    "javax.persistence.fetchgraph",
                    entityManager.getEntityGraph("PostComment.post")
                )
            );
        });
        assertNotNull(comment.getPost());
    }

    @Test
    public void testFindWithNamedEntityLoadGraph() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L,
                Collections.singletonMap(
                    "javax.persistence.loadgraph",
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
    @NamedEntityGraph(name = "PostComment.post", attributeNodes = @NamedAttributeNode("post"))
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

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
