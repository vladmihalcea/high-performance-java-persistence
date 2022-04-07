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
public class JoinedTableTest extends AbstractTest {

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
            Board board = topic.getBoard();
            LOGGER.info("Fetch Topic projection");
            List<String> titles = entityManager.createQuery("""
                select t.title 
                from Topic t 
                where t.board = :board
                """, String.class)
            .setParameter("board", board)
            .getResultList();
            assertEquals(2, titles.size());
        });

        doInJPA(entityManager -> {
            LOGGER.info("Fetch just one Topic");
            Topic _topic = entityManager.find(Topic.class, topic.getId());
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
            .setParameter("id", 1L)
            .getSingleResult();

            assertTrue(board.getTopics().stream().anyMatch(t -> t instanceof Post));
            assertTrue(board.getTopics().stream().anyMatch(t -> t instanceof Announcement));
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

        TopicStatistics statistics = doInJPA(entityManager -> {
            Long topicId = topic.getId();
            LOGGER.info("Fetch one TopicStatistic");
            return entityManager.find(TopicStatistics.class, topicId);
        });

        try {
            statistics.getTopic().getCreatedOn();
        }
        catch (Exception expected) {
            LOGGER.info( "Topic was not fetched" );
        }

        doInJPA(entityManager -> {

            List<Tuple> results = entityManager.createQuery("""
                select count(t), t.class
                from Topic t
                group by t.class
                order by t.class
                """)
            .getResultList();

            assertEquals(2, results.size());
        });

        doInJPA(entityManager -> {
            Board board = topic.getBoard();

            List<Topic> topics = entityManager.createQuery("""
                select t
                from Topic t
                where t.board = :board
                order by t.class
                """, Topic.class)
            .setParameter("board", board)
            .getResultList();

            assertEquals(2, topics.size());
            assertTrue(topics.get(0) instanceof Post);
            assertTrue(topics.get(1) instanceof Announcement);
        });

        doInJPA(entityManager -> {
            Board board = topic.getBoard();

            List<Topic> topics = entityManager.createQuery("""
                select t
                from Topic t
                where t.board = :board
                order by
                   case
                   when type(t) = Announcement then 10
                   when type(t) = Post then 20
                   end
               """, Topic.class)
            .setParameter("board", board)
            .getResultList();

            assertEquals(2, topics.size());
            assertTrue(topics.get(0) instanceof Announcement);
            assertTrue(topics.get(1) instanceof Post);
        });

        doInJPA(entityManager -> {
            Long boardId = topic.getBoard().getId();
            LOGGER.info("Fetch Board topics");

            Board board = entityManager.find(Board.class, boardId);

            List<Topic> topics = board.getTopics();

            assertTrue(topics.stream().anyMatch(t -> t instanceof Post));
            assertTrue(topics.stream().anyMatch(t -> t instanceof Announcement));
        });
    }

    @Test
    public void testQueryUsingAll() {
        doInJPA(entityManager -> {
            Board board1 = new Board();
            board1.setId(1L);
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
            board2.setId(2L);
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
            List<Board> postOnlyBoards = entityManager.createQuery("""
                select distinct b
                from Board b
                where Post = all (
                   select type(t) from Topic t where t.board = b
                )
                """, Board.class)
            .getResultList();
            assertEquals(1, postOnlyBoards.size());
            assertEquals("JPA", postOnlyBoards.get(0).getName());
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
    @Inheritance(strategy = InheritanceType.JOINED)
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
