package com.vladmihalcea.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class EnumOrdinalDescriptionMySQLTest extends AbstractTest {

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
    public void beforeInit() {
        executeStatement("drop table if exists post");
        executeStatement("drop table if exists post_status_info");
        executeStatement("create table post (id integer not null auto_increment, title varchar(100), status tinyint, primary key (id))");
        executeStatement("create table post_status_info (id tinyint not null, name varchar(50), description varchar(255), primary key (id))");
        executeStatement("alter table post add constraint status_id foreign key (status) references post_status_info (id)");
        executeStatement("insert into post_status_info (id, name, description) values (0, 'PENDING', 'Post waiting to be approved')");
        executeStatement("insert into post_status_info (id, name, description) values (1, 'APPROVED', 'Post approved')");
        executeStatement("insert into post_status_info (id, name, description) values (2, 'SPAM', 'Post rejected as spam')");
        executeStatement("insert into post_status_info (id, name, description) values (3, 'REQUIRES_MODERATOR_INTERVENTION', 'Post requires moderator intervention')");
    }

    @Test
    public void testPendingPost() {
        Post _post = doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");
            post.setStatus(PostStatus.PENDING);
            entityManager.persist(post);
            
            return post;
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, _post.getId());

            assertEquals(PostStatus.PENDING, post.getStatus());
            assertEquals("PENDING", post.getStatus().name());

            Tuple tuple = (Tuple) entityManager.createNativeQuery("""
                SELECT
                    p.id, p.title, p.status,
                    psi.name, psi.description
                FROM post p
                INNER JOIN post_status_info psi ON p.status = psi.id
                WHERE p.id = :postId
                """, Tuple.class)
            .setParameter("postId", _post.getId())
            .getSingleResult();

            assertEquals("PENDING", tuple.get("name"));
            assertEquals("Posts waiting to be approved by the admin", tuple.get("description"));
        });
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

        doInJPA(entityManager -> {
            int postId = 50;

            try {
                entityManager.createNativeQuery("""
                    INSERT INTO post (status, title, id)
                    VALUES (:status, :title, :id)
                    """)
                .setParameter("status", 99)
                .setParameter("title", "Illegal Enum value")
                .setParameter("id", postId)
                .executeUpdate();

                fail("Should not allow us to insert an Enum value of 100!");
            } catch (ConstraintViolationException e) {
                assertTrue(e.getMessage().contains("a foreign key constraint fails"));
            }
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
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        private String title;

        @Enumerated(EnumType.ORDINAL)
        @Column(columnDefinition = "tinyint")
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
