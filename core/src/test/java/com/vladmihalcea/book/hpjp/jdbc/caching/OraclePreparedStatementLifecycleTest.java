package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Tuple;
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

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testPreparedStatement() {
        doInJDBC(connection -> {
            assertEquals(0, getOpenCursorsForStatement(INSERT_POST).size());
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(INSERT_POST);
                assertEquals(0, getOpenCursorsForStatement(INSERT_POST).size());

                int index  = 0;
                preparedStatement.setLong(++index, 1L);
                preparedStatement.setString(++index, "High-Performance SQL");

                preparedStatement.executeUpdate();

                List<Tuple> openCursors = getOpenCursorsForStatement(INSERT_POST);
                assertEquals(1, openCursors.size());
            } catch (SQLException e) {
                fail(e.getMessage());
            } finally {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                assertEquals(1, getOpenCursorsForStatement(INSERT_POST).size());
            }
        });
        assertEquals(0, getOpenCursorsForStatement(INSERT_POST).size());
    }

    private List<Tuple> getOpenCursorsForStatement(String sql) {
        return doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                SELECT  
                    sql_text, 
                    count(*) AS "OPEN CURSORS", 
                    user_name 
                FROM v$open_cursor oc
                WHERE 
                    sql_text = :sql
                GROUP BY 
                    sql_text, 
                    user_name 
                ORDER BY 
                    count(*) DESC
                """, Tuple.class)
            .setParameter("sql", sql)
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
