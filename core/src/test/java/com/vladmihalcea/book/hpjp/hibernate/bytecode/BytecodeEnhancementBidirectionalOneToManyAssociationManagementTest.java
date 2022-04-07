package com.vladmihalcea.book.hpjp.hibernate.bytecode;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(
    biDirectionalAssociationManagement = true
)
public class BytecodeEnhancementBidirectionalOneToManyAssociationManagementTest extends AbstractTest {

    //Needed as otherwise we get a No unique field [LOGGER] error
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Test
    public void testSetParentAssociation() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");

            PostComment comment = new PostComment()
                .setId(1L)
                .setReview("Excellent book to understand Java Persistence");

            assertNull(comment.getPost());
            post.setComments(List.of(comment));
            assertSame(post, comment.getPost());

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertEquals("High-Performance Java Persistence", comment.getPost().getTitle());
            assertEquals("Excellent book to understand Java Persistence", comment.getReview());
        });
    }

    @Test
    public void testSetChildAssociation() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");

            PostComment comment = new PostComment()
                .setId(1L)
                .setReview("Excellent book to understand Java Persistence");

            assertFalse(post.getComments().contains(comment));
            comment.setPost(post);
            assertTrue(post.getComments().contains(comment));

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertEquals("High-Performance Java Persistence", comment.getPost().getTitle());
            assertEquals("Excellent book to understand Java Persistence", comment.getReview());
        });
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

        public void setComments(List<PostComment> comments) {
            this.comments = comments;
        }
    }

    @Entity
    @Table(name = "post_comment")
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
