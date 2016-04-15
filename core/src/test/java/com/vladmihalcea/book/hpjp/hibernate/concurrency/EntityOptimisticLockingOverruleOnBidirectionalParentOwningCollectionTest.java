package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.hibernate.annotations.OptimisticLock;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EntityOptimisticLockingOnBidirectionalParentOwningCollectionTest - Test to check optimistic locking overruling on bidirectional parent owning collections
 *
 * @author Vlad Mihalcea
 */
public class EntityOptimisticLockingOverruleOnBidirectionalParentOwningCollectionTest
        extends AbstractEntityOptimisticLockingCollectionTest
        <EntityOptimisticLockingOverruleOnBidirectionalParentOwningCollectionTest.Post, EntityOptimisticLockingOverruleOnBidirectionalParentOwningCollectionTest.Comment> {

    @Entity(name = "post")
    public static class Post  implements AbstractEntityOptimisticLockingCollectionTest.IPost<Comment> {

        @Id
        private Long id;

        private String name;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @OptimisticLock(excluded = true)
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

    @Entity(name = "comment")
    public static class Comment implements AbstractEntityOptimisticLockingCollectionTest.IComment<Post> {

        @Id
        @GeneratedValue(strategy=GenerationType.IDENTITY)
        private Long id;

        private String review;

        @ManyToOne
        @JoinColumn(name = "post_id", insertable = false, updatable = false)
        private Post post;

        public Long getId() {
            return id;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }


        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }

    public EntityOptimisticLockingOverruleOnBidirectionalParentOwningCollectionTest() {
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
        simulateConcurrentTransactions(false);
    }
}
