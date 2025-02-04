package com.vladmihalcea.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class EnumOrdinalMySQLWithoutCheckTest extends AbstractTest {

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
        executeStatement("drop table if exists post");
        executeStatement("create table post (id integer not null auto_increment, title varchar(100), status tinyint unsigned, primary key (id)) engine=InnoDB");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setTitle("Tuning Spring applications with Hypersistence Optimizer")
                    .setStatus(PostStatus.REQUIRES_MODERATOR_INTERVENTION)
            );
        });

        try {
            doInJPA(entityManager -> {
                int postId = 50;

                int rowCount = entityManager.createNativeQuery("""
                    INSERT INTO post (id, title, status)
                    VALUES (:id, :title, :status)
                    """)
                .setParameter("id", postId)
                .setParameter("title", "Illegal Enum value")
                .setParameter("status", 99)
                .executeUpdate();

                assertEquals(1, rowCount);

                Post post = entityManager.find(Post.class, postId);

                fail("Should not map the Enum value of 99!");
            });
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.info("Expected", e);
            assertEquals("Index 99 out of bounds for length 4", e.getMessage());
        }
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM,
        REQUIRES_MODERATOR_INTERVENTION
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(length = 100)
        private String title;

        @Enumerated
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
