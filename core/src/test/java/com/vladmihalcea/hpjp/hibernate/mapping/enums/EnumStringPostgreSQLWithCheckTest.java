package com.vladmihalcea.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class EnumStringPostgreSQLWithCheckTest extends AbstractTest {

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
        return Database.POSTGRESQL;
    }

    @Override
    protected void beforeInit() {
        executeStatement("drop table if exists post cascade");
        executeStatement("drop sequence if exists post_SEQ");
        executeStatement("create sequence post_SEQ start with 1 increment by 50");
        executeStatement("create table post (id integer not null, title varchar(100), status varchar(31), primary key (id))");
    }

    @Test
    public void testCheckConstraint() {
        executeStatement("""
            ALTER TABLE post
            ADD CONSTRAINT CHK_status_enum_value
            CHECK (status in ('PENDING','APPROVED','SPAM','REQUIRES_MODERATOR_INTERVENTION'))
            """);

        try {
            doInJPA(entityManager -> {
                entityManager.createNativeQuery("""
                    INSERT INTO post (id, title, status)
                    VALUES (:id, :title, :status)
                    """)
                    .setParameter("id", 100)
                    .setParameter("title", "Illegal Enum value")
                    .setParameter("status", "UNSUPPORTED")
                    .executeUpdate();

                fail("Should not store the string value of UNSUPPORTED!");
            });
        } catch (ConstraintViolationException e) {
            assertTrue(e.getMessage().contains("new row for relation \"post\" violates check constraint \"chk_status_enum_value\""));
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
            CHECK (status in ('PENDING','APPROVED','SPAM','REQUIRES_MODERATOR_INTERVENTION'))
            """);

        int postCount = 1_000_000;
        int batchSize = 1_000;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        PostStatus[] postStatuses = PostStatus.values();
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(PreparedStatement preparedStatement = connection.prepareStatement("""
                    INSERT INTO post (id, title, status)
                    VALUES (?, ?, ?)
                    """)) {

                    boolean flushed = false;
                    for (int i = 1; i < postCount; i++) {
                        preparedStatement.setInt(1, i);
                        preparedStatement.setString(2, String.format("Post nr %d", i));
                        preparedStatement.setString(3, postStatuses[random.nextInt(postStatuses.length)].name());

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
            CHECK (status in ('PENDING','APPROVED','SPAM','REQUIRES_MODERATOR_INTERVENTION', 'PROMOTED'))
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
        @GeneratedValue
        private Integer id;

        private String title;

        @Enumerated(EnumType.STRING)
        @Column(length = 31)
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
