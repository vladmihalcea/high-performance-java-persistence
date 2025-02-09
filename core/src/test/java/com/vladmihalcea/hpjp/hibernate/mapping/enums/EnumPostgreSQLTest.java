package com.vladmihalcea.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EnumPostgreSQLTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
    }

    @Override
    protected void beforeInit() {
        executeStatement("drop table if exists post cascade");
        executeStatement("drop type if exists PostStatus cascade");
        executeStatement("create type poststatus as enum ('PENDING','APPROVED','REQUIRES_MODERATOR_INTERVENTION','SPAM')");
        //executeStatement("create cast (varchar as PostStatus) with inout as implicit");
        //executeStatement("create cast (PostStatus as varchar) with inout as implicit");
        executeStatement("create table post (id integer not null, title varchar(100), status PostStatus, primary key (id))");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1)
                    .setTitle("High-Performance Java Persistence")
                    .setStatus(PostStatus.PENDING)
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals(PostStatus.PENDING, post.getStatus());
        });
    }

    @Test
    public void testUpdate() {
        Integer postId = doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1)
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
        private Integer id;

        private String title;

        @Enumerated
        @JdbcType(PostgreSQLEnumJdbcType.class)
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
