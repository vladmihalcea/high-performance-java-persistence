package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SingleTableMySQLTriggerTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Board.class,
            Topic.class,
            Post.class,
            Announcement.class,
            TopicStatistics.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement st = connection.createStatement()) {
                    st.executeUpdate("""
                        CREATE
                        TRIGGER post_content_insert_check BEFORE INSERT
                        ON Topic
                        FOR EACH ROW
                        BEGIN
                           IF NEW.DTYPE = 'Post'
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
                        TRIGGER post_content_update_check BEFORE UPDATE
                        ON Topic
                        FOR EACH ROW
                        BEGIN
                           IF NEW.DTYPE = 'Post'
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
                        TRIGGER announcement_validUntil_insert_check BEFORE INSERT
                        ON Topic
                        FOR EACH ROW
                        BEGIN
                           IF NEW.DTYPE = 'Announcement'
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
                    st.executeUpdate("""
                        CREATE
                        TRIGGER announcement_validUntil_update_check BEFORE UPDATE
                        ON Topic
                        FOR EACH ROW
                        BEGIN
                           IF NEW.DTYPE = 'Announcement'
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

            Board board = new Board();
            board.setName("Hibernate");

            entityManager.persist(board);

            Post post = new Post();
            post.setOwner("John Doe");
            post.setTitle("Inheritance");
            post.setContent("Best practices");
            post.setBoard(board);

            entityManager.persist(post);

            Announcement announcement = new Announcement();
            announcement.setOwner("John Doe");
            announcement.setTitle("Release x.y.z.Final");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));
            announcement.setBoard(board);

            entityManager.persist(announcement);
        });

        try {
            doInJPA(entityManager -> {
                Post post = entityManager.createQuery("""
                    select p
                    from Post p
                    where p.content = :content
                    """, Post.class)
                .setParameter("content", "Best practices")
                .getSingleResult();

                post.setContent(null);
            });
            fail("content_check should fail");
        } catch (Exception expected) {
            assertEquals(PersistenceException.class, expected.getCause().getClass());
        }

        try {
            doInJPA(entityManager -> {
                Announcement announcement = entityManager.createQuery("select a from Announcement a", Announcement.class).getSingleResult();
                announcement.setValidUntil(null);
            });
            fail("valid_until_check should fail");
        } catch (Exception expected) {
            assertEquals(PersistenceException.class, expected.getCause().getClass());
        }

        try {
            doInJPA(entityManager -> {
                entityManager.persist(new Post());
            });
            fail("content_check should fail");
        } catch (Exception expected) {
            assertEquals(PersistenceException.class, expected.getCause().getClass());
        }

        try {
            doInJPA(entityManager -> {
                entityManager.persist(new Announcement());
            });
            fail("content_check should fail");
        } catch (Exception expected) {
            assertEquals(PersistenceException.class, expected.getCause().getClass());
        }
    }

    @Entity(name = "Board")
    @Table(name = "board")
    public static class Board {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        @OneToMany(mappedBy = "board")
        private List<Topic> topics = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Topic> getTopics() {
            return topics;
        }
    }

    @Entity(name = "Topic")
    @Table(name = "topic")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    public static class Topic {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        private String owner;

        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn = new Date();

        @ManyToOne(fetch = FetchType.LAZY)
        private Board board;

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

        public Board getBoard() {
            return board;
        }

        public void setBoard(Board board) {
            this.board = board;
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
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

    @Entity(name = "TopicStatistics")
    @Table(name = "topic_statistics")
    public static class TopicStatistics {

        @Id
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Topic topic;

        private long views;

        public TopicStatistics() {}

        public TopicStatistics(Topic topic) {
            this.topic = topic;
        }

        public Long getId() {
            return id;
        }

        public Topic getTopic() {
            return topic;
        }

        public long getViews() {
            return views;
        }

        public void incrementViews() {
            this.views++;
        }
    }
}
