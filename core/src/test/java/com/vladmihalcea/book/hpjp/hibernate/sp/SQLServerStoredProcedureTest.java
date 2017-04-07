package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.regex.Pattern;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.PostComment;
import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerStoredProcedureTest extends AbstractSQLServerIntegrationTest {

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
                try {
                    statement.executeUpdate("DROP PROCEDURE count_comments");
                } catch (SQLException ignore) {

                }
                try {
                    statement.executeUpdate("DROP FUNCTION fn_count_comments");
                } catch (SQLException ignore) {

                }
                try {
                    statement.executeUpdate("DROP PROCEDURE post_comments");
                } catch (SQLException ignore) {

                }
                statement.executeUpdate(
                    "CREATE PROCEDURE count_comments " +
                    "   @postId INT, " +
                    "   @commentCount INT OUTPUT " +
                    "AS " +
                    "BEGIN " +
                    "   SELECT @commentCount = COUNT(*)  " +
                    "   FROM post_comment  " +
                    "   WHERE post_id = @postId " +
                    "END"
                );
                statement.executeUpdate(
                    "CREATE FUNCTION fn_count_comments (@postId INT)  " +
                    "RETURNS INT  " +
                    "AS  " +
                    "BEGIN  " +
                    "    DECLARE @commentCount int;  " +
                    "    SELECT @commentCount = COUNT(*) " +
                    "    FROM post_comment   " +
                    "    WHERE post_id = @postId;  " +
                    "    RETURN(@commentCount);  " +
                    "END"
                );
                statement.executeUpdate(
                    "CREATE PROCEDURE post_comments " +
                    "    @postId INT, " +
                    "    @postComments CURSOR VARYING OUTPUT " +
                    "AS " +
                    "    SET NOCOUNT ON; " +
                    "    SET @postComments = CURSOR " +
                    "    FORWARD_ONLY STATIC FOR " +
                    "        SELECT *  " +
                    "        FROM post_comment   " +
                    "        WHERE post_id = @postId;  " +
                    "    OPEN @postComments;"
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
            StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("count_comments")
                .registerStoredProcedureParameter(
                    "postId", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter(
                    "commentCount", Long.class, ParameterMode.OUT)
                .setParameter("postId", 1L);

            query.execute();

            Long commentCount = (Long) query.getOutputParameterValue("commentCount");
            assertEquals(Long.valueOf(2), commentCount);
        });
    }

    @Test
    public void testStoredProcedureRefCursor() {
        try {
            doInJPA(entityManager -> {
                StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("post_comments")
                .registerStoredProcedureParameter(1, Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter(2, Class.class, ParameterMode.REF_CURSOR)
                .setParameter(1, 1L);

                query.execute();
                List<Object[]> postComments = query.getResultList();
                assertNotNull(postComments);
            });
        } catch (Exception e) {
            assertTrue(Pattern.compile("Dialect .*? not known to support REF_CURSOR parameters").matcher(e.getCause().getMessage()).matches());
        }
    }

    @Test
    public void testStoredProcedureReturnValue() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            int commentCount = session.doReturningWork(connection -> {
                try (CallableStatement function = connection.prepareCall("{ ? = call fn_count_comments(?) }")) {
                    function.registerOutParameter(1, Types.INTEGER);
                    function.setInt(2, 1);
                    function.execute();
                    return function.getInt(1);
                }
            });
            assertEquals(2, commentCount);
        });
    }
}
