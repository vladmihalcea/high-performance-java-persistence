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

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
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
        });
    }

    @Test
    public void testPolymorphicQuery() {
        doInJPA(entityManager -> {
            Board board = entityManager.getReference(Board.class, 1L);

            List<Topic> topics = entityManager.createQuery("""
                select t
                from Topic t
                where t.board = :board
                """, Topic.class)
            .setParameter("board", board)
            .getResultList();

            assertEquals(2, topics.size());
        });
    }

    @Test
    public void testSubclassQuery() {
        doInJPA(entityManager -> {
            Board board = entityManager.getReference(Board.class, 1L);

            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where p.board = :board
                """, Post.class)
            .setParameter("board", board)
            .getResultList();

            assertEquals(1, posts.size());

            return posts.get(0);
        });
    }

    @Test
    public void testPolymorphicAssociation() {
        doInJPA(entityManager -> {
            Board board = entityManager.createQuery("""
                select b
                from Board b
                join fetch b.topics
                where b.id = :id
                """, Board.class)
            .setParameter("id", 1L)
            .getSingleResult();

            assertEquals(2, board.getTopics().size());
        });

        doInJPA(entityManager -> {
            List<TopicStatistics> statistics = entityManager.createQuery("""
                select s
                from TopicStatistics s
                join fetch s.topic t
                """, TopicStatistics.class)
            .getResultList();

            assertEquals(2, statistics.size());
        });
    }

    @Test
    public void testOrderByClassType() {
        doInJPA(entityManager -> {
            Board board = entityManager.getReference(Board.class, 1L);

            List<Topic> topics = entityManager.createQuery("""
                select t
                from Topic t
                where t.board = :board
                order by t.class
                """, Topic.class)
            .setParameter("board", board)
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
    @Inheritance
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
