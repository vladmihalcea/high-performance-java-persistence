package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SingleTableTest extends AbstractTest {

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
        Board _board = doInJPA(entityManager -> {

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

            return board;
        });

        doInJPA(entityManager -> {
            List<Topic> topics = entityManager
            .createQuery(
                "select t " +
                "from Topic t" +
                " where t.board = :board", Topic.class)
            .setParameter("board", _board)
            .getResultList();

            assertEquals(2, topics.size());
        });

        Post _post = doInJPA(entityManager -> {
            List<Post> posts = entityManager
            .createQuery(
                "select p " +
                "from Post p " +
                "where p.board = :board", Post.class)
            .setParameter("board", _board)
            .getResultList();

            assertEquals(1, posts.size());

            return posts.get(0);
        });

        doInJPA(entityManager -> {
            Board board = entityManager
            .createQuery(
                "select b " +
                "from Board b " +
                "join fetch b.topics " +
                "where b.id = :id", Board.class)
            .setParameter("id", _board.getId())
            .getSingleResult();

            assertEquals(2, board.getTopics().size());
        });

        doInJPA(entityManager -> {
            TopicStatistics statistics = entityManager
            .createQuery(
                "select s " +
                "from TopicStatistics s " +
                "join fetch s.topic t " +
                "where t.id = :topicId", TopicStatistics.class)
            .setParameter("topicId", _post.getId())
            .getSingleResult();

            assertTrue(statistics.getTopic() instanceof Post);
        });

        doInJPA(entityManager -> {
            List<Topic> topics = entityManager
            .createQuery(
                "select t " +
                "from Topic t " +
                "where t.board = :board " +
                "order by t.class", Topic.class)
            .setParameter("board", _board)
            .getResultList();

            assertEquals(2, topics.size());

            assertTrue(topics.get(0) instanceof Announcement);
            assertTrue(topics.get(1) instanceof Post);
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
    @Inheritance
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

        @OneToOne
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
