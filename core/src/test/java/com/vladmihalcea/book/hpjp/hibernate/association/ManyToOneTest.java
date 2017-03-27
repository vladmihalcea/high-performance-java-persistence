package com.vladmihalcea.book.hpjp.hibernate.association;

import java.util.List;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class ManyToOneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");
            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            PostComment comment = new PostComment("My review");
            comment.setPost(post);
            entityManager.persist(comment);

            entityManager.flush();
            comment.setPost(null);
        });
    }

    @Test
    public void testThreePostComments() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");
            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class, 1L);
            
            PostComment comment1 = new PostComment( "My first review");
            comment1.setPost( post );
            PostComment comment2 = new PostComment( "My second review");
            comment2.setPost( post );
            PostComment comment3 = new PostComment( "My third review");
            comment3.setPost( post );

            entityManager.persist(comment1);
            entityManager.persist(comment2);
            entityManager.persist(comment3);
        });

        doInJPA(entityManager -> {
            PostComment comment1 = entityManager.getReference(PostComment.class, 2L);

            entityManager.remove(comment1);
        });

        doInJPA(entityManager -> {
            List<PostComment> comments = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "where pc.post.id = :postId", PostComment.class)
            .setParameter( "postId", 1L )
            .getResultList();
        });
    }
    
    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        public Post() {}

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
        @GeneratedValue
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        private Post post;

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
}
