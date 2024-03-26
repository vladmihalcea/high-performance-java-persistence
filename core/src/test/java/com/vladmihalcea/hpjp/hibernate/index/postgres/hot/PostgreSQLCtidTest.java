package com.vladmihalcea.hpjp.hibernate.index.postgres.hot;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLCtidTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Test
    public void testCtid() {
        AtomicInteger revision = new AtomicInteger();
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle(
                        String.format(
                            "High-Performance Java Persistence, revision %d",
                            revision.incrementAndGet()
                        )
                    )
            );
        });
        checkCtid();
        doInJPA(entityManager -> {
            entityManager
                .find(Post.class, 1L)
                .setTitle(
                    String.format(
                        "High-Performance Java Persistence, revision %d",
                        revision.incrementAndGet()
                    )
                );
        });
        checkCtid();
        doInJPA(entityManager -> {
            entityManager
                .find(Post.class, 1L)
                .setTitle(
                    String.format(
                        "High-Performance Java Persistence, revision %d",
                        revision.incrementAndGet()
                    )
                );
        });
        checkCtid();
    }

    private void checkCtid() {
        doInJPA(entityManager -> {
            Tuple tuple = (Tuple) entityManager
                .createNativeQuery("""
                    SELECT 
                        ctid,
                        id,
                        title
                    FROM 
                        post
                    WHERE
                        id = :id
                    """, Tuple.class)
                .setParameter("id", 1L)
                .getSingleResult();

            LOGGER.info(
                "Ctid: {} for post with id: {} and title: {}",
                tuple.get("ctid"),
                tuple.get("id"),
                tuple.get("title")
            );
        });
    }

    @Test
    public void testHOT() {
        AtomicInteger revision = new AtomicInteger();
        checkHeapOnlyTuples();
        while (revision.incrementAndGet() <= 5){
            doInStatelessSession(session -> {
                String title = String.format(
                    "High-Performance Java Persistence, revision %d",
                    revision.get()
                );
                session.upsert(
                    new Post()
                        .setId(1L)
                        .setTitle(title)
                );
            });
        }
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
