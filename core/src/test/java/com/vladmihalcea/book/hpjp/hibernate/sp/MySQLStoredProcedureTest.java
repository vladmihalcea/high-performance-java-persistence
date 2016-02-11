package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Pattern;

import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.Post;
import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.PostComment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <code>MySQLStoredProcedureTest</code> - MySQL StoredProcedure Test
 *
 * @author Vlad Mihalcea
 */
public class MySQLStoredProcedureTest extends AbstractMySQLIntegrationTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Before
    public void init() {
        super.init();
        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP PROCEDURE IF EXISTS count_comments");
            }
            catch (SQLException ignore) {
            }
        });
        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP PROCEDURE IF EXISTS post_comments");
            }
            catch (SQLException ignore) {
            }
        });
        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                    "CREATE PROCEDURE count_comments (" +
                    "   IN postId INT, " +
                    "   OUT commentCount INT " +
                    ") " +
                    "BEGIN " +
                    "    SELECT COUNT(*) INTO commentCount " +
                    "    FROM post_comment  " +
                    "    WHERE post_comment.post_id = postId; " +
                    "END"
                );

                statement.executeUpdate(
                    "CREATE  PROCEDURE post_comments(IN postId INT) " +
                    "BEGIN " +
                    "    SELECT *  " +
                    "    FROM post_comment   " +
                    "    WHERE post_id = postId;  " +
                    "END"
                );
            }
        });
        doInJPA(entityManager -> {
            Post post = new Post(1L);
            post.setTitle("Post");

            PostComment comment1 = new PostComment("Good");
            comment1.setId(1L);
            PostComment comment2 = new PostComment("Excellent");
            comment2.setId(2L);

            post.addComment(comment1);
            post.addComment(comment2);
            entityManager.persist(post);
        });
    }

    @Test
    public void testStoredProcedureOutParameter() {
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("count_comments");
            query.registerStoredProcedureParameter("postId", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("commentCount", Long.class, ParameterMode.OUT);

            query.setParameter("postId", 1L);

            query.execute();
            Long commentCount = (Long) query.getOutputParameterValue("commentCount");
            assertEquals(Long.valueOf(2), commentCount);
        });
    }


    @Test
    public void testStoredProcedureRefCursor() {
        try {
            doInJPA(entityManager -> {
                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("post_comments");
                query.registerStoredProcedureParameter(1, void.class, ParameterMode.REF_CURSOR);
                query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);

                query.setParameter(2, 1L);

                List<Object[]> postComments = query.getResultList();
                assertEquals(2, postComments.size());
            });
        } catch (Exception e) {
            assertTrue(Pattern.compile("Dialect .*? not known to support REF_CURSOR parameters").matcher(e.getCause().getMessage()).matches());
        }
    }

    @Test
    public void testStoredProcedureReturnValue() {
        try {
            doInJPA(entityManager -> {
                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("post_comments");
                query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);

                query.setParameter(1, 1L);

                List<Object[]> postComments = query.getResultList();
                assertEquals(2, postComments.size());
            });
        } catch (Exception e) {
            assertEquals("Dialect [org.hibernate.dialect.MySQL5Dialect] not known to support REF_CURSOR parameters", e.getCause().getMessage());
        }
    }
}
