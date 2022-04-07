package com.vladmihalcea.book.hpjp.hibernate.view;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SubselectTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {

        return new Class<?>[]{
                Post.class,
                PostDetails.class,
                PostComment.class,
                Tag.class,
                DatabaseFunction.class
        };
    }

    @Entity(name = "DatabaseFunction")
    @Immutable
    @Subselect(
        "SELECT " +
        "    functions.routine_name as name, " +
        "    string_agg(functions.data_type, ',') as params " +
        "FROM (" +
        "    SELECT " +
        "        routines.routine_name, " +
        "        parameters.data_type, " +
        "        parameters.ordinal_position " +
        "    FROM " +
        "        information_schema.routines " +
        "    LEFT JOIN " +
        "        information_schema.parameters " +
        "    ON " +
        "        routines.specific_name = parameters.specific_name " +
        "    WHERE " +
        "        routines.specific_schema='public' " +
        "    ORDER BY " +
        "        routines.routine_name, " +
        "        parameters.ordinal_position " +
        ") AS functions " +
        "GROUP BY functions.routine_name"
    )
    public static class DatabaseFunction {

        @Id
        private String name;

        private String params;

        public String getName() {
            return name;
        }

        public String[] getParams() {
            return params.split(",");
        }
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
    }

    @Test
    public void testStoredProcedureOutParameter() {
        doInJPA(entityManager -> {
            List<DatabaseFunction> databaseFunctions = entityManager.createQuery(
                "select df " +
                "from DatabaseFunction df " +
                "where df.name like '%comments' " +
                "order by df.name", DatabaseFunction.class)
            .getResultList();

            DatabaseFunction countComments = databaseFunctions.get(0);
            assertEquals("count_comments", countComments.getName());
            assertEquals(2, countComments.getParams().length);
            assertEquals("bigint", countComments.getParams()[0]);

            DatabaseFunction postComments = databaseFunctions.get(1);
            assertEquals("post_comments",  postComments.getName());
            assertEquals(1,  postComments.getParams().length);
            assertEquals("bigint",  postComments.getParams()[0]);
        });
    }

}
