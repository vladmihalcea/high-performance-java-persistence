package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ManyToOneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("First post");
            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("My first review")
                    .setPost(post)
            );
        });

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);

            comment.setPost(null);
        });

        doInJPA(entityManager -> {
            PostComment comment = entityManager.getReference(PostComment.class, 1L);

            entityManager.remove(comment);
        });
    }

    @Test
    public void testThreePostComments() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("First post");
            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class, 1L);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("My first review")
                    .setPost(post)
            );

            entityManager.persist(
                new PostComment()
                    .setId(2L)
                    .setReview("My second review")
                    .setPost(post)
            );

            entityManager.persist(
                new PostComment()
                    .setId(3L)
                    .setReview("My third review")
                    .setPost(post)
            );
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
                .setParameter("postId", 1L)
                .getResultList();

            assertEquals(2, comments.size());
        });
    }

    @Test
    public void testPersistAndQuery() {
        Post post = new Post();
        post.setId(1L);
        post.setTitle("High-Performance Java Persistence");

        PostComment comment = new PostComment();
        comment.setId(1L);
        comment.setReview("Amazing book!");
        comment.setPost(post);

        doInJPA(entityManager -> {
            entityManager.persist(post);
            entityManager.persist(comment);
        });

        doInJPA(entityManager -> {
            PostComment postComment = entityManager
                .createQuery(
                    "select pc " +
                    "from PostComment pc " +
                    "join fetch pc.post " +
                    "where pc.id = :id", PostComment.class)
                .setParameter("id", comment.getId())
                .getSingleResult();

            assertEquals("High-Performance Java Persistence", postComment.getPost().getTitle());
            assertEquals("Amazing book!", postComment.getReview());

        });
    }

    @Test
    public void testPersistWithFind() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("Amazing book!")
                    .setPost(post)
            );
        });
    }

    @Test
    public void testPersistWithProxy() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class, 1L);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("Amazing book!")
                    .setPost(post)
            );
        });
    }

    @Test
    public void testFetchEntityWithFind() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class, 1L);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("Amazing book!")
                    .setPost(post)
            );
        });

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);

            LOGGER.info(
                "The post '{}' got the following comment '{}'",
                comment.getPost().getTitle(),
                comment.getReview()
            );
        });
    }

    @Test
    public void testFetchEntityWithJoinFetch() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class, 1L);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("Amazing book!")
                    .setPost(post)
            );
        });

        doInJPA(entityManager -> {
            PostComment comment = entityManager.createQuery("""
                select pc
                from PostComment pc
                join fetch pc.post
                where pc.id = :id
                """, PostComment.class)
            .setParameter("id", 1L)
            .getSingleResult();

            LOGGER.info(
                "The post '{}' got the following comment '{}'",
                comment.getPost().getTitle(),
                comment.getReview()
            );
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
    public static class PostComment {

        @Id
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        private Post post;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }
    }
}
