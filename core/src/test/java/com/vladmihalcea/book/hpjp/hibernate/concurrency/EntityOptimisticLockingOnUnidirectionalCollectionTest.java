package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * EntityOptimisticLockingOnUnidirectionalCollectionTest - Test to check optimistic locking on unidirectional collections
 *
 * @author Vlad Mihalcea
 */
public class EntityOptimisticLockingOnUnidirectionalCollectionTest
        extends AbstractEntityOptimisticLockingCollectionTest
        <EntityOptimisticLockingOnUnidirectionalCollectionTest.Post, EntityOptimisticLockingOnUnidirectionalCollectionTest.Comment> {

    @Entity(name = "post")
    public static class Post implements AbstractEntityOptimisticLockingCollectionTest.IPost<Comment> {

        @Id
        private Long id;

        private String name;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderColumn(name = "comment_index")
        private List<Comment> comments = new ArrayList<Comment>();

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Comment> getComments() {
            return comments;
        }

        public final int getVersion() {
            return version;
        }

        public void addComment(Comment comment) {
            comments.add(comment);
        }
    }

    @Entity(name = "comment")
    public static class Comment implements AbstractEntityOptimisticLockingCollectionTest.IComment<Post> {

        @Id
        @GeneratedValue(strategy=GenerationType.IDENTITY)
        private Long id;

        private String review;

        public Long getId() {
            return id;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }

    public EntityOptimisticLockingOnUnidirectionalCollectionTest() {
        super(Post.class, Comment.class);
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Comment.class
        };
    }

    @Test
    public void testOptimisticLocking() {
        try {
            simulateConcurrentTransactions(true);
        } catch (Exception e) {
            LOGGER.info("Expected", e);
            assertTrue(e instanceof OptimisticLockException);
        }
    }
}
