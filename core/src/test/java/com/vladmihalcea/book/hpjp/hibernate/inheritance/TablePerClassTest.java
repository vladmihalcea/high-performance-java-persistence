package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class TablePerClassTest extends AbstractTest {
    
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
        // When using MySQL with older Hibernate versions,
        // UNION is used instead of UNION ALL
        // return Database.MYSQL;
        return Database.POSTGRESQL;
    }

    @Test
    public void test() {
        Topic topic = doInJPA(entityManager -> {
            Board board = new Board()
                .setId(1L)
                .setName("Hibernate");

            entityManager.persist(board);

            Post post = new Post()
                .setOwner("Vlad Mihalcea")
                .setTitle("High-Performance Java Persistence")
                .setContent("Best practices")
                .setBoard(board);

            entityManager.persist(post);

            Announcement announcement = new Announcement()
                .setOwner("Vlad Mihalcea")
                .setTitle("Release 1.2.3")
                .setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)))
                .setBoard(board);

            entityManager.persist(announcement);

            entityManager.persist(
                new TopicStatistics()
                    .setTopic(post)
                    .incrementViews()
            );

            entityManager.persist(
                new TopicStatistics()
                    .setTopic(announcement)
                    .incrementViews()
            );

            return post;
        });

        doInJPA(entityManager -> {
            Board board = topic.getBoard();
            LOGGER.info("Fetch Topics");
            List<Topic> topics = entityManager.createQuery("""
                select t
                from Topic t
                where t.board = :board
                """, Topic.class)
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
            Board board = entityManager.createQuery("""
                select b 
                from Board b 
                join fetch b.topics 
                where b.id = :id
                """, Board.class)
            .setParameter("id", id)
            .getSingleResult();
        });

        doInJPA(entityManager -> {
            Long topicId = topic.getId();
            LOGGER.info("Fetch statistics");
            TopicStatistics statistics = entityManager.createQuery("""
                select s 
                from TopicStatistics s 
                join fetch s.topic t 
                where t.id = :topicId
                """, TopicStatistics.class)
            .setParameter("topicId", topicId)
            .getSingleResult();
        });
    }

    @Entity(name = "Board")
    @Table(name = "board")
    public static class Board {

        @Id
        private Long id;

        private String name;

        @OneToMany(mappedBy = "board")
        private List<Topic> topics = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public Board setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Board setName(String name) {
            this.name = name;
            return this;
        }

        public List<Topic> getTopics() {
            return topics;
        }
    }

    @Entity(name = "Topic")
    @Table(name = "topic")
    @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
    public static class Topic<T extends Topic<T>> {

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

        public T setTitle(String title) {
            this.title = title;
            return (T) this;
        }

        public String getOwner() {
            return owner;
        }

        public T setOwner(String owner) {
            this.owner = owner;
            return (T) this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public T setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return (T) this;
        }

        public Board getBoard() {
            return board;
        }

        public T setBoard(Board board) {
            this.board = board;
            return (T) this;
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post extends Topic<Post> {

        private String content;

        public String getContent() {
            return content;
        }

        public Post setContent(String content) {
            this.content = content;
            return this;
        }
    }

    @Entity(name = "Announcement")
    @Table(name = "announcement")
    public static class Announcement extends Topic<Announcement> {

        @Temporal(TemporalType.TIMESTAMP)
        private Date validUntil;

        public Date getValidUntil() {
            return validUntil;
        }

        public Announcement setValidUntil(Date validUntil) {
            this.validUntil = validUntil;
            return this;
        }
    }

    @Entity(name = "TopicStatistics")
    @Table(name = "topic_statistics")
    public static class TopicStatistics {

        @Id
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        @JoinColumn(name = "id")
        private Topic topic;

        private long views;

        public Long getId() {
            return id;
        }

        public TopicStatistics setId(Long id) {
            this.id = id;
            return this;
        }

        public Topic getTopic() {
            return topic;
        }

        public TopicStatistics setTopic(Topic topic) {
            this.topic = topic;
            return this;
        }

        public long getViews() {
            return views;
        }

        public TopicStatistics incrementViews() {
            this.views++;
            return this;
        }
    }
}
