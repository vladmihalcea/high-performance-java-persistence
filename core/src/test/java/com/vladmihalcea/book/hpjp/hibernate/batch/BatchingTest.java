package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * BatchingTest - Test to check the JDBC batch support
 *
 * @author Vlad Mihalcea
 */
public class BatchingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "5");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    @Test
    public void testInsertPosts() {
        LOGGER.info("testInsertPosts");
        insertPosts();
    }

    @Test
    public void testInsertPostsAndComments() {
        LOGGER.info("testInsertPostsAndComments");
        insertPostsAndComments();
    }

    @Test
    public void testUpdatePosts() {
        insertPosts();

        LOGGER.info("testUpdatePosts");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p     
                """, Post.class)
            .getResultList();

            posts.forEach(post -> post.setTitle(post.getTitle().replaceAll("no", "nr")));
        });
    }

    @Test
    public void testUpdatePostsAndComments() {
        insertPostsAndComments();

        LOGGER.info("testUpdatePostsAndComments");
        doInJPA(entityManager -> {
            entityManager.createQuery("""
                select c
                from PostComment c
                join fetch c.post  
                """, PostComment.class)
            .getResultList()
            .forEach(c -> {
                c.setReview(c.getReview().replaceAll("Good", "Very good"));
                Post post = c.getPost();
                post.setTitle(post.getTitle().replaceAll("no", "nr"));
            });
        });
    }

    @Test
    public void testDeletePosts() {
        insertPosts();

        LOGGER.info("testDeletePosts");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p     
                """, Post.class)
            .getResultList();

            posts.forEach(entityManager::remove);
        });
    }

    @Test
    public void testDeletePostsAndComments() {
        insertPostsAndComments();

        LOGGER.info("testDeletePostsAndComments");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                """, Post.class)
            .getResultList();

            posts.forEach(entityManager::remove);
        });
    }

    @Test
    public void testDeletePostsAndCommentsWithManualChildRemoval() {
        insertPostsAndComments();

        LOGGER.info("testDeletePostsAndCommentsWithManualChildRemoval");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                """, Post.class)
            .getResultList();

            for (Post post : posts) {
                for (Iterator<PostComment> commentIterator = post.getComments().iterator();
                        commentIterator.hasNext(); ) {
                    PostComment comment = commentIterator.next();
                    comment.setPost(null);
                    commentIterator.remove();
                }
            }
            entityManager.flush();
            posts.forEach(entityManager::remove);
        });
    }

    private void insertPosts() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Post no. %d", i))
                );
            }
        });
    }

    private void insertPostsAndComments() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Post no. %d", i))
                        .addComment(new PostComment("Good"))
                );
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
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
    public static class PostComment {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

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
