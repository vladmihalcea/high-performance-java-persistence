package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * VersionedBatchingTest - Test to check the JDBC batch support for versioned entities
 *
 * @author Vlad Mihalcea
 */
public class VersionedBatchingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "5");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "false");
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
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p ", Post.class)
            .getResultList();

            posts.forEach(post -> post.setTitle(post.getTitle().replaceAll("no", "nr")));
        });
    }

    @Test
    public void testUpdatePostsAndComments() {
        insertPostsAndComments();

        LOGGER.info("testUpdatePostsAndComments");
        doInJPA(entityManager -> {
            List<PostComment> comments = entityManager.createQuery(
                "select c " +
                "from PostComment c " +
                "join fetch c.post ", PostComment.class)
            .getResultList();

            comments.forEach(comment -> {
                comment.setReview(comment.getReview().replaceAll("Good", "Very good"));
                Post post = comment.getPost();
                post.setTitle(post.getTitle().replaceAll("no", "nr"));
            });
        });
    }

    @Test
    public void testDeletePosts() {
        insertPosts();

        LOGGER.info("testDeletePosts");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p ", Post.class)
            .getResultList();

            posts.forEach(entityManager::remove);
        });
    }

    @Test
    public void testDeletePostsAndComments() {
        insertPostsAndComments();

        LOGGER.info("testDeletePostsAndComments");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.comments ", Post.class)
            .getResultList();

            posts.forEach(entityManager::remove);
        });
    }

    private void insertPosts() {
        doInJPA(entityManager -> {
            for (int i = 0; i < 3; i++) {
                entityManager.persist(new Post(String.format("Post no. %d", i + 1)));
            }
        });
    }

    private void insertPostsAndComments() {
        doInJPA(entityManager -> {
            for (int i = 0; i < 3; i++) {
                Post post = new Post(String.format("Post no. %d", i));
                post.addComment(new PostComment("Good"));
                entityManager.persist(post);
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String title;

        @Version
        private int version;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
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
