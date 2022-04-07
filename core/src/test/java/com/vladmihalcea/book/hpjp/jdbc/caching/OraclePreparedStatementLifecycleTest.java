package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class OraclePreparedStatementLifecycleTest extends AbstractOracleIntegrationTest {

    public static final String INSERT_POST = "INSERT INTO post (id, title) VALUES (:1 , :2 )";
    public static final String INSERT_POST_PREFIX = "INSERT INTO post";

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testPreparedStatement() {
        doInJDBC(connection -> {
            // This setting doesn't influence the outcome
            executeStatement("ALTER SESSION SET session_cached_cursors=0");
            assertEquals(0, getOpenCursorsForStatement(INSERT_POST_PREFIX).size());
            PreparedStatement preparedStatement1 = null;
            PreparedStatement preparedStatement2 = null;
            try {
                preparedStatement1 = connection.prepareStatement(INSERT_POST);
                assertEquals(0, getOpenCursorsForStatement(INSERT_POST_PREFIX).size());

                int index  = 0;
                preparedStatement1.setLong(++index, 1L);
                preparedStatement1.setString(++index, "High-Performance SQL");
                preparedStatement1.executeUpdate();

                preparedStatement2 = connection.prepareStatement("INSERT INTO post (id) VALUES (:1)");
                preparedStatement2.setLong(1, 2L);
                preparedStatement2.executeUpdate();

                List<Tuple> openCursors = getOpenCursorsForStatement(INSERT_POST_PREFIX);
                assertEquals(2, openCursors.size());
            } catch (SQLException e) {
                fail(e.getMessage());
            } finally {
                if (preparedStatement1 != null) {
                    preparedStatement1.close();
                }
                if (preparedStatement2 != null) {
                    preparedStatement2.close();
                }
                assertEquals(2, getOpenCursorsForStatement(INSERT_POST_PREFIX).size());
                connection.commit();
                assertEquals(0, getOpenCursorsForStatement(INSERT_POST_PREFIX).size());
            }
        });
    }

    private List<Tuple> getOpenCursorsForStatement(String sqlPrefix) {
        return doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                SELECT  
                    sql_text, 
                    count(*) AS "OPEN CURSORS"
                FROM v$open_cursor oc
                WHERE 
                    user_name= 'ORACLE' AND 
                    sql_text LIKE :sqlPrefix
                GROUP BY 
                    sql_text, 
                    user_name 
                ORDER BY 
                    count(*) DESC
                """, Tuple.class)
            .setParameter("sqlPrefix", sqlPrefix + "%")
            .getResultList();
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}
