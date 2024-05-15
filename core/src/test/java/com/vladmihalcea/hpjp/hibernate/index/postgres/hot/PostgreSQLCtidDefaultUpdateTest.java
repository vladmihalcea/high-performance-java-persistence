package com.vladmihalcea.hpjp.hibernate.index.postgres.hot;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLCtidDefaultUpdateTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }
    @Test
    public void testHOTDefaultUpdate() {
        AtomicInteger revision = new AtomicInteger();
        checkHeapOnlyTuples();
        while (revision.incrementAndGet() <= 5){
            doInJPA(session -> {
                Post post = new Post()
                    .setId((long) revision.get())
                    .setTitle(
                        String.format(
                            "High-Performance Java Persistence, revision %d",
                            revision.get()
                        )
                    );
                session.persist(post);
            });
        }

        executeStatement(
            "DROP INDEX IF EXISTS idx_post_created_on",
            """
            CREATE INDEX IF NOT EXISTS idx_post_created_on ON post (created_on)
            """,
            "ANALYZE VERBOSE"
        );
        doInJPA(entityManager -> {
            for (long id = 1; id <= 5; id++) {
                Post post = entityManager.find(Post.class, id);
                post.setTitle("Changed");
            }
        });

        checkHeapOnlyTuples();
    }

    private void checkHeapOnlyTuples() {
        doInJDBC(connection -> {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("""
                    SELECT n_tup_upd, n_tup_hot_upd
                    FROM pg_stat_user_tables
                    WHERE relname = 'post'
                    """
                );
                while (resultSet.next()) {
                    int i = 0;
                    long n_tup_upd = resultSet.getLong(++i);
                    long n_tup_hot_upd = resultSet.getLong(++i);

                    LOGGER.info(
                        "n_tup_upd: {}, n_tup_hot_upd: {}",
                        n_tup_upd,
                        n_tup_hot_upd
                    );
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private LocalDateTime createdOn = LocalDateTime.now();

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
