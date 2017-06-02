package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class IntegerDiscriminatorDefaultTest extends AbstractMySQLIntegrationTest {

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
        Topic _topic = doInJPA(entityManager -> {

            addConsistecyTriggers( entityManager );

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
            Board board = _topic.getBoard();
            LOGGER.info("Fetch Topics");
            List<Topic> topics = entityManager
                    .createQuery("select t from Topic t where t.board = :board", Topic.class)
                    .setParameter("board", board)
                    .getResultList();

            for ( Topic topic: topics ) {
                LOGGER.info( "Found topic: {}", topic.getClass().getName() );
            }
        });
    }

    private void addConsistecyTriggers(EntityManager entityManager) {
        entityManager.unwrap(Session.class).doWork( connection -> {
            try(Statement st = connection.createStatement()) {
                st.executeUpdate(
                        "CREATE " +
                                "TRIGGER post_content_check BEFORE INSERT " +
                                "ON Topic " +
                                "FOR EACH ROW " +
                                "BEGIN " +
                                "   IF NEW.topic_type_id = 1 " +
                                "   THEN " +
                                "       IF NEW.content IS NULL " +
                                "       THEN " +
                                "           signal sqlstate '45000' " +
                                "           set message_text = 'Post content cannot be NULL'; " +
                                "       END IF; " +
                                "   END IF; " +
                                "END;"
                );
                st.executeUpdate(
                        "CREATE " +
                                "TRIGGER announcement_validUntil_check BEFORE INSERT " +
                                "ON Topic " +
                                "FOR EACH ROW " +
                                "BEGIN " +
                                "   IF NEW.topic_type_id = 2 " +
                                "   THEN " +
                                "       IF NEW.validUntil IS NULL " +
                                "       THEN " +
                                "           signal sqlstate '45000' " +
                                "           set message_text = 'Announcement validUntil cannot be NULL'; " +
                                "       END IF; " +
                                "   END IF; " +
                                "END;"
                );
            }
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
    @DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "topic_type_id",
        columnDefinition = "TINYINT(1)"
    )
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
    @Table(name = "announcement")
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
