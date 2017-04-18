package com.vladmihalcea.book.hpjp.hibernate.fetching;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HibernateProxyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }

    @Test
    public void test() {
        Post _post = doInJPA(entityManager -> {
            Post post = new Post();
            post.setId( 1L );
            post.setTitle( "High-Performance Java Persistence" );
            entityManager.persist(post);
            return post;
        });

        doInJPA(entityManager -> {
            LOGGER.info( "Saving a PostComment" );

            Post post = entityManager.getReference(Post.class, 1L);

            PostComment comment = new PostComment();
            comment.setId( 1L );
            comment.setPost( post );
            comment.setReview( "A must read!" );
            entityManager.persist( comment );
        });

        doInJPA(entityManager -> {
            LOGGER.info( "Loading a PostComment" );

            PostComment comment = entityManager.find(
                PostComment.class,
                1L
            );

            LOGGER.info( "Loading the Post Proxy" );

            assertEquals(
                "High-Performance Java Persistence",
                comment.getPost().getTitle()
            );
        });

        doInJPA(entityManager -> {
            LOGGER.info( "Equality check" );
            Post post = entityManager.getReference(Post.class, 1L);
            LOGGER.info( "Post entity class: {}", post.getClass().getName() );

            assertFalse(_post.equals(post));

            assertTrue(_post.equals( Hibernate.unproxy( post)));
        });
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

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Post)) return false;
            return id != null && id.equals(((Post) o).id);
        }

        @Override
        public int hashCode() {
            return 31;
        }
    }

    @Entity(name = "PostComment")
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
