package com.vladmihalcea.hpjp.hibernate.view;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ViewTest extends AbstractPostgreSQLIntegrationTest {

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

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.hbm2ddl.auto", "none");
    }

    @Entity(name = "DatabaseFunction")
    @Immutable
    @Table(name = "database_functions")
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

    @Override
    protected void beforeInit() {
        executeStatement("DROP FUNCTION IF EXISTS fn_count_comments(bigint)");
        executeStatement("DROP FUNCTION IF EXISTS fn_post_comments(bigint)");
        executeStatement("""
            CREATE OR REPLACE FUNCTION fn_count_comments(
               IN postId bigint,
               OUT commentCount bigint)
               RETURNS bigint AS
            $BODY$
                BEGIN
                    SELECT COUNT(*) INTO commentCount
                    FROM post_comment
                    WHERE post_id = postId;
                END;
            $BODY$
            LANGUAGE plpgsql;
            """);
        executeStatement("""
            CREATE OR REPLACE FUNCTION fn_post_comments(postId BIGINT)
               RETURNS REFCURSOR AS
            $BODY$
                DECLARE
                    postComments REFCURSOR;
                BEGIN
                    OPEN postComments FOR
                        SELECT *
                        FROM post_comment
                        WHERE post_id = postId;
                    RETURN postComments;
                END;
            $BODY$
            LANGUAGE plpgsql
            """);
        executeStatement("""
            CREATE OR REPLACE VIEW database_functions AS
            SELECT
                functions.routine_name as name,
                string_agg(functions.data_type, ',') as params
            FROM (
                SELECT
                    routines.routine_name,
                    parameters.data_type,
                    parameters.ordinal_position
                FROM
                    information_schema.routines
                LEFT JOIN
                    information_schema.parameters ON
                    routines.specific_name = parameters.specific_name
                WHERE
                    routines.specific_schema='public' and
                    routines.routine_name LIKE 'fn_%_comments'
                ORDER BY routines.routine_name, parameters.ordinal_position
            ) AS functions
            GROUP BY functions.routine_name
            """);
    }

    @Test
    public void testStoredProcedureOutParameter() {
        doInJPA(entityManager -> {
            List<DatabaseFunction> databaseFunctions = entityManager.createQuery("""
                SELECT df
                FROM DatabaseFunction df
                """, DatabaseFunction.class)
            .getResultList();

            DatabaseFunction countComments = databaseFunctions.get(0);
            assertEquals("fn_count_comments", countComments.getName());
            assertEquals(2, countComments.getParams().length);
            assertEquals("bigint", countComments.getParams()[0]);

            DatabaseFunction postComments = databaseFunctions.get(1);
            assertEquals("fn_post_comments", postComments.getName());
            assertEquals(1, postComments.getParams().length);
            assertEquals("bigint", postComments.getParams()[0]);
        });
    }
}
