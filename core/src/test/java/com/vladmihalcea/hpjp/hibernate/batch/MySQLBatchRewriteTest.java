package com.vladmihalcea.hpjp.hibernate.batch;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.*;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class MySQLBatchRewriteTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "10");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider()
            .setRewriteBatchedStatements(true);
        /*return new MySQLDataSourceProvider();*/
    }

    @Test
    public void testInsertPostsUsingStatement() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    String INSERT = "insert into post (id, title) values (%1$d, 'Post no. %1$d')";
                    for (long id = 1; id <= 10; id++) {
                        statement.addBatch(
                            String.format(INSERT, id)
                        );
                    }
                    statement.executeBatch();
                }
            });
        });
    }

    @Test
    public void testInsertPosts() {
        insertPosts();
    }

    @Test
    public void testInsertPostsAndComments() {
        insertPostsAndComments();
    }

    @Test
    public void testUpdatePosts() {
        insertPosts();

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.comments
                """, Post.class)
            .getResultList();

            posts.forEach(post -> post.setTitle(post.getTitle().replaceAll("no", "nr")));
        });
    }

    @Test
    public void testUpdatePostsAndComments() {
        insertPostsAndComments();

        doInJPA(entityManager -> {
            List<PostComment> comments = entityManager.createQuery("""
                select c
                from PostComment c
                join fetch c.post
                """, PostComment.class)
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

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.comments
                """, Post.class)
            .getResultList();

            posts.forEach(entityManager::remove);
        });
    }

    @Test
    public void testDeletePostsAndComments() {
        insertPostsAndComments();

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.comments
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
                left join fetch p.comments
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
            for (long i = 1; i <= 10; i++) {
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
                        .addComment(
                            new PostComment()
                                .setId(i)
                                .setReview("Good")
                        )
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
        private Long id;

        @ManyToOne
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
