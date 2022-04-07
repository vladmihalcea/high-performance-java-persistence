package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.FastOracleDialect;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.PostComment;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OracleStoredProcedureTest extends AbstractOracleIntegrationTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        List<Class> entities = new ArrayList<>(Arrays.asList(entityProvider.entities()));
        entities.add(QueryHolder.class);
        return entities.toArray(new Class[]{});
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
                statement.executeUpdate(
                    "CREATE OR REPLACE FUNCTION fn_post_and_comments ( " +
                    "    postId IN NUMBER ) " +
                    "    RETURN SYS_REFCURSOR " +
                    "IS " +
                    "    postAndComments SYS_REFCURSOR; " +
                    "BEGIN " +
                    "   OPEN postAndComments FOR " +
                    "        SELECT " +
                    "            p.id AS \"p.id\", " +
                    "            p.title AS \"p.title\", " +
                    "            p.version AS \"p.version\", " +
                    "            c.id AS \"c.id\", " +
                    "            c.post_id AS \"c.post_id\", " +
                    "            c.version AS \"c.version\", " +
                    "            c.review AS \"c.review\" " +
                    "       FROM post p " +
                    "       JOIN post_comment c ON p.id = c.post_id " +
                    "       WHERE p.id = postId; " +
                    "   RETURN postAndComments; " +
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

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return OracleDialect.class.getName();
            }
        };
    }

    @Test
    public void testStoredProcedureOutParameter() {
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("count_comments")
                .registerStoredProcedureParameter(1, Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter(2, Long.class, ParameterMode.OUT)
                .setParameter(1, 1L);

            try {
                query.execute();

                Long commentCount = (Long) query.getOutputParameterValue(2);
                assertEquals(Long.valueOf(2), commentCount);
            } finally {
                query.unwrap(ProcedureOutputs.class).release();
            }
        });
    }

    @Test
    public void testStoredProcedureRefCursor() {
        doInJPA(entityManager -> {
            StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("post_comments")
                .registerStoredProcedureParameter(1, Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter(2, Class.class, ParameterMode.REF_CURSOR)
                .setParameter(1, 1L);

            try {
                query.execute();

                List<Object[]> postComments = query.getResultList();
                assertEquals(2, postComments.size());
            } finally {
                query.unwrap(ProcedureOutputs.class).release();
            }
        });
    }

    @Test
    public void testHibernateProcedureCallRefCursor() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            ProcedureCall call = session.createStoredProcedureCall("post_comments");
            call.registerParameter(1, Long.class, ParameterMode.IN);
            call.registerParameter(2, Class.class, ParameterMode.REF_CURSOR);

            call.setParameter(1, 1L);
            ProcedureOutputs outputs = call.getOutputs();
            try {
                Output output = outputs.getCurrent();

                if (output.isResultSet()) {
                    List<Object[]> postComments = ((ResultSetOutput) output).getResultList();
                    assertEquals(2, postComments.size());
                }
            } finally {
                outputs.release();
            }
        });
    }

    @Test
    public void testFunction() {
        doInJPA(entityManager -> {
            BigDecimal commentCount = (BigDecimal) entityManager
                .createNativeQuery("SELECT fn_count_comments(:postId) FROM DUAL")
                .setParameter("postId", 1L)
                .getSingleResult();

            assertEquals(BigDecimal.valueOf(2), commentCount);
        });
    }

    @Test
    public void testFunctionCallAfterRegistration() {
        doInJPA(entityManager -> {
            Integer commentCount = (Integer) entityManager
                .createQuery("select fn_count_comments(:postId) from Post where id = :postId")
                .setParameter("postId", 1L)
                .getSingleResult();

            assertEquals(Integer.valueOf(2), commentCount);
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

    @Test
    public void testStoredProcedureRefCursorWithJDBC() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try (CallableStatement function = connection.prepareCall(
                        "{ call post_comments(?, ?) }" )) {
                    function.setInt( 1, 1 );
                    //function.registerOutParameter( 2, OracleTypes.CURSOR );
                    function.registerOutParameter( 2, -10 );
                    function.execute();
                    try (ResultSet resultSet = (ResultSet) function.getObject(2);) {
                        while (resultSet.next()) {
                            Long postCommentId = resultSet.getLong(1);
                            String review = resultSet.getString(2);
                        }
                    }
                }
            } );
        });
    }

    @Test
    public void testNamedNativeQueryStoredProcedureRefCursor() {
        doInJPA(entityManager -> {
            List<Object[]> postAndComments = entityManager
            .createNamedQuery(
                "fn_post_and_comments")
            .setParameter(1, 1L)
            .getResultList();
            Object[] postAndComment = postAndComments.get(0);
            Post post = (Post) postAndComment[0];
            PostComment comment = (PostComment) postAndComment[1];
            assertEquals(2, postAndComments.size());
        });
    }

    @Test
    public void testNamedNativeQueryStoredProcedureRefCursorWithJDBC() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            session.doWork( connection -> {
                try (CallableStatement function = connection.prepareCall(
                        "{ ? = call fn_post_and_comments( ? ) }" )) {
                    //function.registerOutParameter( 1, OracleTypes.CURSOR );
                    function.registerOutParameter( 1, -10 );
                    function.setInt( 2, 1 );
                    function.execute();
                    try (ResultSet resultSet = (ResultSet) function.getObject(1);) {
                        while (resultSet.next()) {
                            Long postCommentId = resultSet.getLong(1);
                            String review = resultSet.getString(2);
                        }
                    }
                }
            } );
        });
    }

    @Entity(name = "QueryHolder")
    @NamedNativeQuery(
        name = "fn_post_and_comments",
        query = "{ ? = call fn_post_and_comments( ? ) }",
        callable = true,
        resultSetMapping = "post_and_comments"
    )
    @SqlResultSetMapping(
        name = "post_and_comments",
        entities = {
            @EntityResult(
                entityClass = Post.class,
                fields = {
                    @FieldResult(name = "id", column = "p.id"),
                    @FieldResult(name = "title", column = "p.title"),
                    @FieldResult(name = "version", column = "p.version"),
                }
            ),
            @EntityResult(
                entityClass = PostComment.class,
                fields = {
                    @FieldResult(name = "id", column = "c.id"),
                    @FieldResult(name = "post", column = "c.post_id"),
                    @FieldResult(name = "version", column = "c.version"),
                    @FieldResult(name = "review", column = "c.review"),
                }
            )
        }
    )
    public static class QueryHolder {
        @Id private Long id;
    }

    public static class OracleDialect extends FastOracleDialect {

        @Override
        public void initializeFunctionRegistry(QueryEngine queryEngine) {
            super.initializeFunctionRegistry(queryEngine);
            queryEngine.getSqmFunctionRegistry().register(
                "fn_count_comments",
                new StandardSQLFunction("fn_count_comments", StandardBasicTypes.INTEGER)
            );
        }
    }

}
