package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import java.math.BigDecimal;
import java.sql.Statement;

import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.Post;
import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.PostComment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * <code>OracleStoredProcedureTest</code> - Oracle StoredProcedure Test
 *
 * @author Vlad Mihalcea
 */
public class OracleStoredProcedureTest extends AbstractOracleXEIntegrationTest {

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
                statement.executeUpdate(
                    "CREATE OR REPLACE PROCEDURE count_comments (  " +
                    "   postId IN NUMBER,  " +
                    "   commentCount OUT NUMBER )  " +
                    "AS  " +
                    "BEGIN  " +
                    "    SELECT COUNT(*) INTO commentCount  " +
                    "    FROM post_comment  " +
                    "    WHERE post_id = postId; " +
                    "END;"
                );
                statement.executeUpdate(
                    "CREATE OR REPLACE PROCEDURE post_comments ( " +
                    "   postId IN NUMBER, " +
                    "   postComments OUT SYS_REFCURSOR ) " +
                    "AS  " +
                    "BEGIN " +
                    "    OPEN postComments FOR " +
                    "    SELECT *" +
                    "    FROM post_comment " +
                    "    WHERE post_id = postId; " +
                    "END;"
                );
                statement.executeUpdate(
                    "CREATE OR REPLACE FUNCTION fn_count_comments ( " +
                    "    postId IN NUMBER ) " +
                    "    RETURN NUMBER " +
                    "IS " +
                    "    commentCount NUMBER; " +
                    "BEGIN " +
                    "    SELECT COUNT(*) INTO commentCount " +
                    "    FROM post_comment " +
                    "    WHERE post_id = postId; " +
                    "    RETURN( commentCount ); " +
                    "END;"
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

    @Test @Ignore("https://hibernate.atlassian.net/browse/HHH-9286")
    public void testStoredProcedureRefCursor() {
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("post_comments");
            query.registerStoredProcedureParameter(0, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(1, Class.class, ParameterMode.REF_CURSOR);

            query.setParameter(0, 1L);

            query.execute();
            Object postComments = query.getOutputParameterValue("postComments");
            assertNotNull(postComments);
        });
    }

    @Test
    public void testStoredProcedureReturnValue() {
        doInJPA(entityManager -> {
            BigDecimal commentCount = (BigDecimal) entityManager
                .createNativeQuery("SELECT fn_count_comments(:postId) FROM DUAL")
                .setParameter("postId", 1L)
                .getSingleResult();
            assertEquals(BigDecimal.valueOf(2), commentCount);
        });
    }
}
