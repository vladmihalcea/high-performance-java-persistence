package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class ExtraSQLInjectionTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Good");

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent");

            post.addComment(comment1);
            post.addComment(comment2);
            entityManager.persist(post);
        });
    }

    @Test
    @Ignore
    public void testStatementUpdateDropTable() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertEquals("Good", comment.getReview());
        });

        updatePostCommentReviewUsingStatement(1L, "Awesome");

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertEquals("Awesome", comment.getReview());
        });

        try {
            updatePostCommentReviewUsingStatement(1L, "'; DROP TABLE post_comment; -- '");
        } catch (Exception e) {
            LOGGER.error("Failure", e);
        }

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertNotNull(comment);
        });
    }

    @Test
    @Ignore
    public void testPreparedStatementUpdateDropTable() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertEquals("Good", comment.getReview());
        });

        updatePostCommentReviewUsingPreparedStatement(1L, "Awesome");

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertEquals("Awesome", comment.getReview());
        });

        try {
            updatePostCommentReviewUsingPreparedStatement(1L, "'; DROP TABLE post_comment; -- '");
        } catch (Exception e) {
            LOGGER.error("Failure", e);
        }

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertNotNull(comment);
        });
    }

    @Test
    public void testPreparedStatementSelectAndWait() {
        assertEquals("Good", getPostCommentReviewUsingPreparedStatement("1"));
        try {
            getPostCommentReviewUsingPreparedStatement("1 AND 1 >= ALL ( SELECT 1 FROM pg_locks, pg_sleep(2) )");
        } catch (Exception expected) {
            LOGGER.error("Failure", expected);
        }
        assertEquals("Good", getPostCommentReviewUsingPreparedStatement("1"));
    }

    @Test
    public void testStatementSelectDropTable() {
        assertEquals("Good", getPostCommentReviewUsingStatement("1"));
        try {
            getPostCommentReviewUsingStatement("1; DROP TABLE post_comment");
        } catch (Exception expected) {
            LOGGER.error("Failure", expected);
        }
        assertEquals("Good", getPostCommentReviewUsingStatement("1"));
    }

    @Test
    public void testPreparedStatementSelectDropTable() {
        assertEquals("Good", getPostCommentReviewUsingPreparedStatement("1"));
        try {
            getPostCommentReviewUsingPreparedStatement("1; DROP TABLE post_comment");
        } catch (Exception expected) {
            LOGGER.error("Failure", expected);
        }
        assertEquals("Good", getPostCommentReviewUsingPreparedStatement("1"));
    }

    @Test
    @Ignore
    public void testGetPostCommentByReview() {
        getPostCommentByReview("1 AND 1 >= ALL ( SELECT 1 FROM pg_locks, pg_sleep(2) )");
    }

    private void updatePostCommentReviewUsingStatement(Long id, String review) {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                        "UPDATE post_comment " +
                        "SET review = '" + review + "' " +
                        "WHERE id = " + id);
                }
            });
        });
    }

    private void updatePostCommentReviewUsingPreparedStatement(Long id, String review) {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                try(PreparedStatement statement = connection.prepareStatement(
                    "UPDATE post_comment " +
                    "SET review = '" + review + "' " +
                    "WHERE id = " + id
                )) {
                    statement.executeUpdate();
                }
            });
        });
    }

    private String getPostCommentReviewUsingStatement(String id) {
        return doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            return session.doReturningWork(connection -> {
                String sql =
                    "SELECT review " +
                    "FROM post_comment " +
                    "WHERE id = " + id;
                try(Statement statement = connection.createStatement()) {
                    try(ResultSet resultSet = statement.executeQuery(sql)) {
                        return resultSet.next() ? resultSet.getString(1) : null;
                    }
                }
            });
        });
    }

    private String getPostCommentReviewUsingPreparedStatement(String id) {
        return doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            return session.doReturningWork(connection -> {
                String sql =
                    "SELECT review " +
                    "FROM post_comment " +
                    "WHERE id = " + id;
                try(PreparedStatement statement = connection.prepareStatement(sql)) {
                    try(ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? resultSet.getString(1) : null;
                    }
                }
            });
        });
    }

    private PostComment getPostCommentByReview(String review) {
        return doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select p " +
                "from PostComment p " +
                "where p.review = :review", PostComment.class)
            .setParameter("review", review)
            .getSingleResult();
        });
    }

    @Test
    @Ignore
    public void testSelectAllEntities() {
        doInJPA(entityManager -> {
            List<Post> posts = findAll("com.vladmihalcea.book.hpjp.hibernate.forum.Post");
            posts = findAll("java.lang.Object");
            posts.size();
        });
    }

    @Test
    public void testGetPostByTitleSuccess() {
        doInJPA(entityManager -> {
            List<Post> posts = getPostsByTitle("High-Performance Java Persistence");
        });
    }

    @Test
    public void testPostGetByTitleAndWait() {
        doInJPA(entityManager -> {
            List<Post> posts = getPostsByTitle(
                "High-Performance Java Persistence' and " +
                "FUNCTION('1 >= ALL ( SELECT 1 FROM pg_locks, pg_sleep(2) ) --',) is '"
            );
            assertEquals(1, posts.size());
        });
    }

    @Test
    public void testTuples() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = getTuples();
            assertEquals(1, tuples.size());
        });
    }

    private List<Post> getPostsByTitle(String title) {
        return doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where" +
                "   p.title = '" + title + "'", Post.class)
            .getResultList();
        });
    }

    private List<Tuple> getTuples() {
        return doInJPA(entityManager -> {
            Class<Post> entityClass = Post.class;
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Tuple> query = cb.createTupleQuery();
            Root<?> root = query.from(entityClass);
            query.select(
                cb.tuple(
                    root.get("id"),
                    cb.function("now", Date.class)
                )
            );

            return entityManager.createQuery(query).getResultList();
        });
    }

    private  <T> List<T> findAll(String entityName) {
        return (List<T>) doInJPA(entityManager -> {
            try {
                return entityManager.unwrap(Session.class).createQuery(
                    "select e " +
                    "from " + Class.forName(entityName).getName() + " e ")
                .getResultList();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
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
