package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class StringDiscriminatorTest extends AbstractMySQLIntegrationTest {

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
        doInJPA(this::addConsistencyTriggers);
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

    private void addConsistencyTriggers(EntityManager entityManager) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("""
                    CREATE
                    TRIGGER post_content_check BEFORE INSERT
                    ON Topic
                    FOR EACH ROW
                    BEGIN
                       IF NEW.topic_type_id = 'PST'
                       THEN
                           IF NEW.content IS NULL
                           THEN
                               signal sqlstate '45000'
                               set message_text = 'Post content cannot be NULL';
                           END IF;
                       END IF;
                    END;
                    """
                );
                st.executeUpdate("""
                    CREATE
                    TRIGGER announcement_validUntil_check BEFORE INSERT
                    ON Topic
                    FOR EACH ROW
                    BEGIN
                       IF NEW.topic_type_id = 'ANN'
                       THEN
                           IF NEW.validUntil IS NULL
                           THEN
                               signal sqlstate '45000'
                               set message_text = 'Announcement validUntil cannot be NULL';
                           END IF;
                       END IF;
                    END;
                    """
                );
            }
        });
    }

    @Entity(name = "Topic")
    @Table(name = "topic")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    @DiscriminatorColumn(
        discriminatorType = DiscriminatorType.STRING,
        name = "topic_type_id",
        columnDefinition = "VARCHAR(3)"
    )
    @DiscriminatorValue("TPC")
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
    @Table(name = "post")
    @DiscriminatorValue("PST")
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
    @Table(name = "announcement")
    @DiscriminatorValue("ANN")
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
