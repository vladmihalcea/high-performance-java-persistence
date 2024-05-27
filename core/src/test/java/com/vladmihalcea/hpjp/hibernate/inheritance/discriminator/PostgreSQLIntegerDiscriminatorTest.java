package com.vladmihalcea.hpjp.hibernate.inheritance.discriminator;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.junit.Test;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLIntegerDiscriminatorTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Topic.class,
            Post.class,
            Announcement.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(this::addConsistencyCheck);
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setOwner("John Doe");
            post.setTitle("Inheritance");
            post.setContent("Best practices");

            entityManager.persist(post);

            Announcement announcement = new Announcement();
            announcement.setOwner("John Doe");
            announcement.setTitle("Release x.y.z.Final");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));

            entityManager.persist(announcement);
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                """, Post.class)
            .getResultList();

            assertEquals(1, posts.size());
        });
    }

    private void addConsistencyCheck(EntityManager entityManager) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("""
                        ALTER TABLE topic
                        ADD CONSTRAINT post_content_check CHECK
                        (
                            CASE
                                WHEN topic_type_id = 1 THEN
                                CASE
                                   WHEN content IS NOT NULL
                                   THEN 1
                                   ELSE 0
                                   END
                                ELSE 1
                            END = 1
                        )
                        """
                );
                st.executeUpdate("""
                        ALTER TABLE topic
                        ADD CONSTRAINT announcement_validUntil_check CHECK
                        (
                            CASE
                                WHEN topic_type_id = 2 THEN
                                CASE
                                   WHEN validUntil IS NOT NULL
                                   THEN 1
                                   ELSE 0
                                   END
                                ELSE 1
                            END = 1
                        )
                        """
                );
            }
        });
    }

    @Entity(name = "Topic")
    @Table(name = "topic")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    @DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "topic_type_id",
        columnDefinition = "NUMERIC(1)"
    )
    @DiscriminatorValue("0")
    public static class Topic {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        private String owner;

        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn = new Date();

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

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }
    }

    @Entity(name = "Post")
    @DiscriminatorValue("1")
    public static class Post extends Topic {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    @Entity(name = "Announcement")
    @DiscriminatorValue("2")
    public static class Announcement extends Topic {

        @Temporal(TemporalType.TIMESTAMP)
        private Date validUntil;

        public Date getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(Date validUntil) {
            this.validUntil = validUntil;
        }
    }
}
