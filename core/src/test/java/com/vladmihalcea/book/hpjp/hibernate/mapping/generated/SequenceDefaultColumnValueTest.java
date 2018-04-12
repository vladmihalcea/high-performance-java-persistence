package com.vladmihalcea.book.hpjp.hibernate.mapping.generated;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.tuple.ValueGenerator;
import org.junit.Test;

import javax.persistence.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SequenceDefaultColumnValueTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    public void init() {
        DataSource dataSource = newDataSource();
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try {
                    statement.executeUpdate(
                        "DROP SEQUENCE sensor_seq"
                    );
                } catch (SQLException ignore) {
                }
                statement.executeUpdate(
                    "CREATE SEQUENCE " +
                    "   sensor_seq " +
                    "START 100"
                );
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        super.init();
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("select p from Post p", Post.class).getSingleResult();

            assertEquals(Long.valueOf(100), post.getSequenceId());
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Column(
            columnDefinition = "int8 DEFAULT nextval('sensor_seq')",
            insertable = false
        )
        private Long sequenceId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Long getSequenceId() {
            return sequenceId;
        }

        public void setSequenceId(Long sequenceId) {
            this.sequenceId = sequenceId;
        }
    }
}
