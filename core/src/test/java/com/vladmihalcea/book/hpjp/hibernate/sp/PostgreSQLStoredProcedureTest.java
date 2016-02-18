package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
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
import java.util.concurrent.atomic.AtomicReference;

import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.Post;
import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.PostComment;
import static org.junit.Assert.assertEquals;

/**
 * <code>PostgreSQLStoredProcedureTest</code> - PostgreSQL StoredProcedure Test
 *
 * @author Vlad Mihalcea
 */
public class PostgreSQLStoredProcedureTest extends AbstractPostgreSQLIntegrationTest {

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
                statement.executeUpdate("DROP FUNCTION count_comments(bigint)");
            }
            catch (SQLException ignore) {
            }
        });
        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP FUNCTION post_comments(bigint)");
            }
            catch (SQLException ignore) {
            }
        });
        doInJDBC(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                    "CREATE OR REPLACE FUNCTION count_comments( " +
                    "   IN postId bigint, " +
                    "   OUT commentCount bigint) " +
                    "   RETURNS bigint AS " +
                    "$BODY$ " +
                    "    BEGIN " +
                    "        SELECT COUNT(*) INTO commentCount " +
                    "        FROM post_comment  " +
                    "        WHERE post_id = postId; " +
                    "    END; " +
                    "$BODY$ " +
                    "LANGUAGE plpgsql;"
                );

                statement.executeUpdate(
                    "CREATE OR REPLACE FUNCTION post_comments(postId BIGINT) " +
                    "   RETURNS REFCURSOR AS " +
                    "$BODY$ " +
                    "    DECLARE " +
                    "        postComments REFCURSOR; " +
                    "    BEGIN " +
                    "        OPEN postComments FOR  " +
                    "            SELECT *  " +
                    "            FROM post_comment   " +
                    "            WHERE post_id = postId;  " +
                    "        RETURN postComments; " +
                    "    END; " +
                    "$BODY$ " +
                    "LANGUAGE plpgsql"
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
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("post_comments");
            query.registerStoredProcedureParameter(1, void.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);

            query.setParameter(2, 1L);

            List<Object[]> postComments = query.getResultList();
            assertEquals(2, postComments.size());
        });
    }

    @Test
    public void testFunctionWithJDBC() {
        doInJPA(entityManager -> {
            final AtomicReference<Long> commentCount = new AtomicReference<>();
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try (CallableStatement function = connection.prepareCall(
                        "{ ? = call count_comments(?) }" )) {
                    function.registerOutParameter( 1, Types.BIGINT );
                    function.setLong( 2, 1L );
                    function.execute();
                    commentCount.set( function.getLong( 1 ) );
                }
            } );
            assertEquals(Long.valueOf(2), commentCount.get());
        });
    }

    @Test
    public void testFunctionWithJDBCByName() {
        doInJPA(entityManager -> {
            final AtomicReference<Long> commentCount = new AtomicReference<>();
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try (CallableStatement function = connection.prepareCall(
                        "{ ? = call count_comments(?) }" )) {
                    function.registerOutParameter( "commentCount", Types.BIGINT );
                    function.setLong( "postId", 1L );
                    function.execute();
                    commentCount.set( function.getLong( 1 ) );
                }
            } );
            assertEquals(Long.valueOf(2), commentCount.get());
        });
    }

    @Test
    public void test_hql_bit_length_function_example() {
        doInJPA(entityManager -> {
            //tag::hql-bit-length-function-example[]
            List<Number> bits = entityManager.createQuery(
                    "select bit_length( c.title ) " +
                            "from Post c ", Number.class )
                    .getResultList();
            //end::hql-bit-length-function-example[]
            assertEquals(2, bits.size());
        });
    }
}
