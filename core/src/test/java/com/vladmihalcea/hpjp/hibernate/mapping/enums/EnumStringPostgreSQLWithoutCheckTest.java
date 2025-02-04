package com.vladmihalcea.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class EnumStringPostgreSQLWithoutCheckTest extends AbstractTest {

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
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setTitle("Check out my website")
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
                    .setParameter("status", "UNSUPPORTED")
                    .executeUpdate();

                assertEquals(1, rowCount);

                Post post = entityManager.find(Post.class, postId);

                fail("Should not map the Enum value of UNSUPPORTED!");
            });
        } catch (Exception e) {
            LOGGER.info("Expected", e);
            assertEquals(
                String.format(
                    "No enum constant %s.UNSUPPORTED",
                    PostStatus.class.getName()
                ).replaceAll("\\$", "."),
                e.getMessage()
            );
        }
    }

    @Test
    public void testUpdate() {
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
