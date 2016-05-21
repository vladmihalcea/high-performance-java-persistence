package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.hibernate.annotations.Parent;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * EntityOptimisticLockingOnComponentCollectionTest - Test to check optimistic locking on component collections
 *
 * @author Vlad Mihalcea
 */
public class EntityOptimisticLockingOnComponentCollectionTest
        extends AbstractEntityOptimisticLockingCollectionTest
        <EntityOptimisticLockingOnComponentCollectionTest.Post, EntityOptimisticLockingOnComponentCollectionTest.Comment> {

    @Entity(name = "post")
    public static class Post implements AbstractEntityOptimisticLockingCollectionTest.IPost<Comment> {

        @Id
        private Long id;

        private String name;

        @ElementCollection
        @JoinTable(name = "post_comments", joinColumns = @JoinColumn(name = "post_id"))
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
            comment.setPost(this);
            comments.add(comment);
        }
    }

    @Embeddable
    public static class Comment implements AbstractEntityOptimisticLockingCollectionTest.IComment<Post> {

        @Parent
        private Post post;

        @Column(name = "review")
        private String review;

        public Post getPost() {
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

    public EntityOptimisticLockingOnComponentCollectionTest() {
        super(Post.class, Comment.class);
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
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
