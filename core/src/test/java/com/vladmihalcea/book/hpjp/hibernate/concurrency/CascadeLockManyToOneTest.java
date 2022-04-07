package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.*;


/**
 * CascadeLockTest - Test to check CascadeType.LOCK
 *
 * @author Vlad Mihalcea
 */
public class CascadeLockManyToOneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class
        };
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("Hibernate Master Class");
            entityManager.persist(post);

            PostComment comment = new PostComment("Good post!");
            comment.setId(1L);
            comment.setPost(post);
            entityManager.persist(comment);
        });
    }

    @Test
    public void testCascadeLockOnDetachedEntityUninitializedWithScope() {
        LOGGER.info("Test lock cascade for detached entity with scope");

        //Load the Post entity, which will become detached
        PostComment comment = doInJPA(entityManager -> (PostComment) entityManager.find(PostComment.class, 1L));

        doInJPA(entityManager -> {
            LOGGER.info("Reattach and lock entity with associations not initialized");
            entityManager.unwrap(Session.class)
                    .buildLockRequest(
                            new LockOptions(LockMode.PESSIMISTIC_WRITE))
                    .setScope(true)
                    .lock(comment);

            LOGGER.info("Check entities are reattached");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        private String body;

        @Version
        private int version;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

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
    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @Cascade(CascadeType.LOCK)
        private Post post;

        private String review;

        @Version
        private int version;

        public PostComment() {}

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

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
}
