package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JoinedTableDiscriminatorTest extends AbstractTest {

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
    }

    @Test
    public void testQueryUsingAll() {
        doInJPA(entityManager -> {
            Board board1 = new Board();
            board1.setName("Hibernate");

            entityManager.persist(board1);

            Post post1 = new Post();
            post1.setOwner("John Doe");
            post1.setTitle("Inheritance");
            post1.setContent("Best practices");
            post1.setBoard(board1);

            entityManager.persist(post1);

            Announcement announcement1 = new Announcement();
            announcement1.setOwner("John Doe");
            announcement1.setTitle("Release x.y.z.Final");
            announcement1.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));
            announcement1.setBoard(board1);

            entityManager.persist(announcement1);

            Board board2 = new Board();
            board2.setName("JPA");

            entityManager.persist(board2);

            Post post2 = new Post();
            post2.setOwner("John Doe");
            post2.setTitle("Inheritance");
            post2.setContent("Best practices");
            post2.setBoard(board2);

            entityManager.persist(post2);

            Post post3 = new Post();
            post3.setOwner("John Doe");
            post3.setTitle("Inheritance");
            post3.setContent("More best practices");
            post3.setBoard(board2);

            entityManager.persist(post3);
        });

        doInJPA(entityManager -> {
            List<Board> postOnlyBoards = entityManager
            .createQuery(
                "select distinct b " +
                "from Board b " +
                "where Post = all (" +
                "   select type(t) from Topic t where t.board = b" +
                ")", Board.class)
            .getResultList();
            assertEquals(1, postOnlyBoards.size());
            assertEquals("JPA", postOnlyBoards.get(0).getName());
        });
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
    @Inheritance(strategy = InheritanceType.JOINED)
    @DiscriminatorColumn(name="class_type")
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
