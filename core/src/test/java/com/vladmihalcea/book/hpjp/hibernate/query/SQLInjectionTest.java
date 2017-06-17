package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.book.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.junit.Test;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class SQLInjectionTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
                PostDetails.class,
                PostComment.class,
                Tag.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Tag tag = new Tag();
            tag.setId(1L);
            tag.setName("Java");
            entityManager.persist(tag);

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
    public void testSelectCustomFunction() {
        /*doInJPA(entityManager -> {
            Post post = findPersonByFirstAndLastName("Vlad", "Mihalcea' and FUNCTION('current_database',) is not null and '' = '");
            LOGGER.info("Found entity {}", post);
        });*/
    }

    @Test
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
    public void testGetPostCommentByReview() {
        getPostCommentByReview("1 AND 1 >= ALL ( SELECT 1 FROM pg_locks, pg_sleep(10) )");
    }

    @Test
    public void testPreparedStatementSelectAndWait() {
        assertEquals("Good", getPostCommentReviewUsingPreparedStatement("1"));
        try {
            getPostCommentReviewUsingPreparedStatement("1 AND 1 >= ALL ( SELECT 1 FROM pg_locks, pg_sleep(10) )");
        } catch (Exception expected) {
            LOGGER.error("Failure", expected);
        }
        assertEquals("Good", getPostCommentReviewUsingPreparedStatement("1"));
    }

    public void updatePostCommentReviewUsingStatement(Long id, String review) {
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

    public void updatePostCommentReviewUsingPreparedStatement(Long id, String review) {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                String sql =
                    "UPDATE post_comment " +
                    "SET review = '" + review + "' " +
                    "WHERE id = " + id;
                try(PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.executeUpdate();
                }
            });
        });
    }

    public String getPostCommentReviewUsingStatement(String id) {
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

    public String getPostCommentReviewUsingPreparedStatement(String id) {
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

    public PostComment getPostCommentByReview(String review) {
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
                "FUNCTION('1 >= ALL ( SELECT 1 FROM pg_locks, pg_sleep(10) ) --',) is '"
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

    public List<Post> getPostsByTitle(String title) {
        return doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where" +
                "   p.title = '" + title + "'", Post.class)
            .getResultList();
        });
    }

    public List<Tuple> getTuples() {
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

    /*public Person findPersonByFirstAndLastName(String firstName, String lastName) {
        return doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select p " +
                "from Person p " +
                "where" +
                "   p.firstName = '" + firstName + "'" +
                "   and p.lastName = '" +lastName + "'", Person.class)
            .getSingleResult();
        });
    }*/

    public <T> List<T> findAll(String entityName) {
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
}
