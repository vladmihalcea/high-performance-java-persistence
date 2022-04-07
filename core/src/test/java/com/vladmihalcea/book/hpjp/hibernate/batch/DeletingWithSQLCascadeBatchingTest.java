package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * DeletingWithoutCascadeBatchingTest - Test to check the JDBC batch support for delete
 *
 * @author Vlad Mihalcea
 */
public class DeletingWithSQLCascadeBatchingTest extends AbstractTest {

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
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    @Test
    public void testDeletePostsAndCommentsWithSQLCascade() {
        insertPostsAndComments();

        LOGGER.info("testDeletePostsAndCommentsWithSQLCascade");
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
    public void testDeletePostsAndCommentsWithSQLCascadeAndManagedChildren() {
        insertPostsAndComments();

        LOGGER.info("testDeletePostsAndCommentsWithSQLCascadeAndManagedChildren");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p ", Post.class)
            .getResultList();

            List<PostComment> comments = entityManager.createQuery(
                "select c " +
                "from PostComment c " +
                "where c.post in :posts", PostComment.class)
            .setParameter("posts", posts)
            .getResultList();

            posts.forEach(entityManager::remove);

            comments.forEach(comment -> comment.setReview("Excellent"));
        });
    }

    @Test
    public void testDeletePostsAndCommentsWithSQLCascadeAndManagedChildrenFloating() {
        insertPostsAndComments();

        LOGGER.info("testDeletePostsAndCommentsWithSQLCascadeAndManagedChildrenFloating");
        try {
            doInJPA(entityManager -> {
                List<Post> posts = entityManager.createQuery(
                    "select p " +
                    "from Post p ", Post.class)
                .getResultList();

                List<PostComment> comments = entityManager.createQuery(
                    "select c " +
                    "from PostComment c " +
                    "where c.post in :posts", PostComment.class)
                .setParameter("posts", posts)
                .getResultList();

                posts.forEach(entityManager::remove);
                entityManager.flush();

                comments.forEach(comment -> comment.setReview("Excellent"));
            });
        } catch (PersistenceException e) {
            assertEquals(OptimisticLockException.class, e.getCause().getClass());
        }
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

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "post")
        @OnDelete(action = OnDeleteAction.CASCADE)
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
        @org.hibernate.annotations.ForeignKey(name = "fk_post_comment_post")
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
