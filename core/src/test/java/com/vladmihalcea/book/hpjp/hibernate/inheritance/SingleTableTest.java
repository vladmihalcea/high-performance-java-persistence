package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.*;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SingleTableTest extends AbstractPostgreSQLIntegrationTest {

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

    @Test
    public void test() {
        Topic topic = doInJPA(entityManager -> {

            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement st = connection.createStatement()) {
                    st.executeUpdate(
                        "ALTER TABLE Topic " +
                        "ADD CONSTRAINT post_content_check CHECK " +
                        "( " +
                        "    CASE " +
                        "        WHEN DTYPE = 'Post' THEN " +
                        "        CASE " +
                        "           WHEN content IS NOT NULL " +
                        "           THEN 1 " +
                        "           ELSE 0 " +
                        "           END " +
                        "        ELSE 1 " +
                        "    END = 1 " +
                        ")"
                    );
                    st.executeUpdate(
                        "ALTER TABLE Topic " +
                        "ADD CONSTRAINT announcement_validUntil_check CHECK " +
                        "( " +
                        "    CASE " +
                        "        WHEN DTYPE = 'Announcement' THEN " +
                        "        CASE " +
                        "           WHEN validUntil IS NOT NULL " +
                        "           THEN 1 " +
                        "           ELSE 0 " +
                        "           END " +
                        "        ELSE 1 " +
                        "    END = 1 " +
                        ")"
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

            TopicStatistics postStatistics = new TopicStatistics(post);
            postStatistics.incrementViews();
            entityManager.persist(postStatistics);

            TopicStatistics announcementStatistics = new TopicStatistics(announcement);
            announcementStatistics.incrementViews();
            entityManager.persist(announcementStatistics);

            return post;
        });

        doInJPA(entityManager -> {
            Board board = topic.getBoard();
            LOGGER.info("Fetch Topics");
            List<Topic> topics = entityManager
                    .createQuery("select t from Topic t where t.board = :board", Topic.class)
                    .setParameter("board", board)
                    .getResultList();
        });

        doInJPA(entityManager -> {
            LOGGER.info("Fetch Board topics");
            entityManager.find(Board.class, topic.getBoard().getId()).getTopics().size();
        });

        doInJPA(entityManager -> {
            LOGGER.info("Fetch Board topics eagerly");
            Long id = topic.getBoard().getId();
            Board board = entityManager.createQuery(
                "select b from Board b join fetch b.topics where b.id = :id", Board.class)
                .setParameter("id", id)
                .getSingleResult();
        });

        doInJPA(entityManager -> {
            Long topicId = topic.getId();
            LOGGER.info("Fetch statistics");
            TopicStatistics statistics = entityManager
                    .createQuery("select s from TopicStatistics s join fetch s.topic t where t.id = :topicId", TopicStatistics.class)
                    .setParameter("topicId", topicId)
                    .getSingleResult();
        });

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
            fail("announcement_validUntil_check should fail");
        } catch (Exception expected) {
            assertEquals(PersistenceException.class, expected.getCause().getClass());
        }

        doInJPA(entityManager -> {
            Board board = topic.getBoard();
            LOGGER.info("Fetch Posts");
            List<Post> posts = entityManager
            .createQuery(
                "select p " +
                "from Post p " +
                "where p.board = :board", Post.class)
            .setParameter("board", board)
            .getResultList();
        });
    }

    @Entity(name = "Board")
    @Table(name = "board")
    public static class Board {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        //Only useful for the sake of seeing the queries being generated.
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
        @GeneratedValue
        private Long id;

        @OneToOne
        @JoinColumn(name = "id")
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
