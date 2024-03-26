package com.vladmihalcea.hpjp.hibernate.index.postgres.hot;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLCtidVersionIndexTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Override
    protected void afterInit() {
        executeStatement(
            "DROP INDEX IF EXISTS idx_post_version",
            """
            CREATE INDEX IF NOT EXISTS idx_post_version ON post (version)
            """,
            "VACUUM FULL"
        );
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

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private short version;

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
