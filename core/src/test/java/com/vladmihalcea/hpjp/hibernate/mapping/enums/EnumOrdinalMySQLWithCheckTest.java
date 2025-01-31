package com.vladmihalcea.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.Time;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class EnumOrdinalMySQLWithCheckTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected void beforeInit() {
        executeStatement("DROP TABLE IF EXISTS post");
        executeStatement("CREATE TABLE post (id integer not null auto_increment, title varchar(100), status tinyint unsigned, PRIMARY KEY (id)) engine=InnoDB");
    }

    @Test
    public void testEnumName() {
        String enumName = PostStatus.APPROVED.name();
        PostStatus enumValue = PostStatus.valueOf(enumName);

        assertSame(PostStatus.APPROVED, enumValue);
    }

    @Test
    public void testEnumOrdinal() {
        int enumOrdinal = PostStatus.APPROVED.ordinal();
        PostStatus enumValue = PostStatus.values()[enumOrdinal];

        assertSame(PostStatus.APPROVED, enumValue);
    }

    @Test
    public void test() {
        Integer postId = doInJPA(entityManager -> {
            Post post = new Post()
                .setTitle("Tuning Spring applications with Hypersistence Optimizer")
                .setStatus(PostStatus.PENDING);

            entityManager.persist(post);

            return post.getId();
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, postId);
            post.setStatus(PostStatus.REQUIRES_MODERATOR_INTERVENTION);
        });
    }

    @Test
    public void testCheckConstraint() {
        executeStatement("""
            ALTER TABLE post
            ADD CONSTRAINT CHK_status_enum_value
            CHECK (status between 0 and 3)
            """);

        try {
            doInJPA(entityManager -> {
                entityManager.createNativeQuery("""
                    INSERT INTO post (title, status)
                    VALUES (:title, :status)
                    """)
                .setParameter("title", "Illegal Enum value")
                .setParameter("status", 99)
                .executeUpdate();

                fail("Should not store the ordinal value of 99!");
            });
        } catch (JDBCException e) {
            assertTrue(e.getMessage().contains("Check constraint 'post_status_enum' is violated"));
        }
    }

    @Test
    public void testAlterColumnPerformance() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        executeStatement("""
            ALTER TABLE post
            ADD CONSTRAINT CHK_status_enum_value
            CHECK (status between 0 and 3)
            """);

        int postCount = 1_000_000;
        int batchSize = 1_000;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        PostStatus[] postStatuses = PostStatus.values();
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(PreparedStatement preparedStatement = connection.prepareStatement("""
                    INSERT INTO post (title, status)
                    VALUES (?, ?)
                    """)) {

                    boolean flushed = false;
                    for (int i = 1; i < postCount; i++) {
                        preparedStatement.setString(1, String.format( "Post nr %d", i ));
                        preparedStatement.setInt(2, random.nextInt(postStatuses.length));

                        flushed = false;
                        preparedStatement.addBatch();
                        if(i % batchSize == 0) {
                            preparedStatement.executeBatch();
                            flushed = true;
                        }
                    }
                    if (!flushed) {
                        preparedStatement.executeBatch();
                    }
                }
            });
        });
        long startNanos = System.nanoTime();
        executeStatement("""
            ALTER TABLE post
            DROP CONSTRAINT CHK_status_enum_value;
            """,
            """
            ALTER TABLE post
            ADD CONSTRAINT CHK_status_enum_value
            CHECK (status between 0 and 4)
            """
        );
        LOGGER.info("Add CHECK constraint took [{}] ms", TimeUnit.NANOSECONDS.toMillis(
            System.nanoTime() - startNanos
        ));
        //Add CHECK constraint took [6221] ms
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM,
        REQUIRES_MODERATOR_INTERVENTION
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @DynamicUpdate
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(length = 100)
        private String title;

        @Enumerated(EnumType.ORDINAL)
        @Column(columnDefinition = "tinyint unsigned")
        private PostStatus status;

        public Integer getId() {
            return id;
        }

        public Post setId(Integer id) {
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

        public PostStatus getStatus() {
            return status;
        }

        public Post setStatus(PostStatus status) {
            this.status = status;
            return this;
        }
    }
}
