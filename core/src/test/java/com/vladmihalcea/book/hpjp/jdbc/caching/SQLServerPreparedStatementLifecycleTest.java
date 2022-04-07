package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerPreparedStatementLifecycleTest extends AbstractSQLServerIntegrationTest {

    public static final String SELECT_POST = "SELECT id, title FROM post";

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testPreparedStatement() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("High-Performance Java Persistence, part %d", i))
                );
            }
        });
        doInJDBC(connection -> {
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                preparedStatement = connection.prepareStatement(SELECT_POST);

                resultSet = preparedStatement.executeQuery();

                resultSet.close();

                resultSet = preparedStatement.executeQuery();

                resultSet.close();

                resultSet = preparedStatement.executeQuery();
            } catch (SQLException e) {
                fail(e.getMessage());
            } finally {
                if(resultSet != null) {
                    resultSet.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
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
