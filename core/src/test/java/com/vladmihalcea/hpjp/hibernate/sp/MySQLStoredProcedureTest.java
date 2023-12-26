package com.vladmihalcea.hpjp.hibernate.sp;

import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.hibernate.Session;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.junit.Before;
import org.junit.Test;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.regex.Pattern;

import static com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider.PostComment;
import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class MySQLStoredProcedureTest extends AbstractMySQLIntegrationTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected void beforeInit() {
        executeStatement("DROP PROCEDURE IF EXISTS count_comments");
        executeStatement("DROP PROCEDURE IF EXISTS post_comments");
        executeStatement("DROP PROCEDURE IF EXISTS fn_count_comments");
        executeStatement("DROP PROCEDURE IF EXISTS getStatistics");
        executeStatement("""
            CREATE PROCEDURE count_comments (
               IN postId INT,
               OUT commentCount INT
            )
            BEGIN
                SELECT COUNT(*) INTO commentCount
                FROM post_comment
                WHERE post_comment.post_id = postId;
            END
            """);
        executeStatement("""
            CREATE PROCEDURE post_comments(IN postId INT)
            BEGIN
                SELECT *
                FROM post_comment
                WHERE post_id = postId;
            END
            """);
        executeStatement("""
            CREATE FUNCTION fn_count_comments(postId integer)
            RETURNS integer
            DETERMINISTIC
            READS SQL DATA
            BEGIN
                DECLARE commentCount integer;
                SELECT COUNT(*) INTO commentCount
                FROM post_comment
                WHERE post_comment.post_id = postId;
                RETURN commentCount;
            END
            """);
        executeStatement("""
            CREATE PROCEDURE getStatistics (
               OUT A BIGINT UNSIGNED,
               OUT B BIGINT UNSIGNED,
               OUT C BIGINT UNSIGNED
            )
            BEGIN
                SELECT count(*) into A from post;
                SELECT count(*) into B from post_comment;
                SELECT count(*) into C from tag;
            END
            """);
    }

    @Override
    protected void afterInit() {
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
    public void testHibernateProcedureCallOutParameter() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            ProcedureCall call = session.createStoredProcedureCall("count_comments");
            call.registerParameter("postId", Long.class, ParameterMode.IN);
            call.registerParameter("commentCount", Long.class, ParameterMode.OUT);

            call.setParameter("postId", 1L);

            Long commentCount = (Long) call.getOutputs().getOutputParameterValue("commentCount");
            assertEquals(Long.valueOf(2), commentCount);
        });
    }

    @Test
    public void testProcedureCallMultipleOutParameter() {
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("getStatistics")
                .registerStoredProcedureParameter(
                        "A", Long.class, ParameterMode.OUT)
                .registerStoredProcedureParameter(
                        "B", Long.class, ParameterMode.OUT)
                .registerStoredProcedureParameter(
                        "C", Long.class, ParameterMode.OUT);

            query.execute();

            Long a = (Long) query
                    .getOutputParameterValue("A");
            Long b = (Long) query
                    .getOutputParameterValue("B");
            Long c = (Long) query
                    .getOutputParameterValue("C");
        });
    }

    @Test
    public void testStoredProcedureRefCursor() {
        try {
            doInJPA(entityManager -> {
                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("post_comments");
                query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter(2, Class.class, ParameterMode.REF_CURSOR);
                query.setParameter(1, 1L);

                query.execute();
                List<Object[]> postComments = query.getResultList();
                assertNotNull(postComments);
            });
        } catch (Exception e) {
            assertTrue(Pattern.compile("Dialect .*? not known to support REF_CURSOR parameters").matcher(e.getMessage()).matches());
        }
    }

    @Test
    public void testStoredProcedureReturnValue() {
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("post_comments");
            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);

            query.setParameter(1, 1L);

            List<Object[]> postComments = query.getResultList();
            assertEquals(2, postComments.size());
        });
    }

    @Test
    public void testHibernateProcedureCallReturnValueParameter() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            ProcedureCall call = session.createStoredProcedureCall("post_comments");
            call.registerParameter(1, Long.class, ParameterMode.IN);
            call.setParameter(1, 1L);

            Output output = call.getOutputs().getCurrent();
            if (output.isResultSet()) {
                List<Object[]> postComments = ((ResultSetOutput) output).getResultList();
                assertEquals(2, postComments.size());
            }
        });
    }

    @Test
    public void testFunctionWithJDBC() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            Integer commentCount = session.doReturningWork( connection -> {
                try (CallableStatement function = connection.prepareCall(
                        "{ ? = call fn_count_comments(?) }" )) {
                    function.registerOutParameter( 1, Types.INTEGER );
                    function.setInt( 2, 1 );
                    function.execute();
                    return function.getInt( 1 );
                }
            } );
            assertEquals(Integer.valueOf(2), commentCount);
        });
    }
}
