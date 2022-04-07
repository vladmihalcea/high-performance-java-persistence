package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MappedSuperclassTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Board.class,
            Post.class,
            Announcement.class,
            PostStatistics.class,
            AnnouncementStatistics.class
        };
    }

    @Override
    protected Database database() {
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
                new PostStatistics()
                    .setTopic(post)
                    .incrementViews()
            );

            entityManager.persist(
                new AnnouncementStatistics()
                    .setTopic(announcement)
                    .incrementViews()
            );

            return post;
        });

        doInJPA(entityManager -> {
            Board board = topic.getBoard();

            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where p.board = :board
                """, Post.class)
            .setParameter("board", board)
            .getResultList();
        });

        doInJPA(entityManager -> {
            Long postId = topic.getId();
            LOGGER.info("Fetch statistics");
            PostStatistics postStatistics = entityManager.createQuery("""
                select ps
                from PostStatistics ps
                join fetch ps.topic t
                where t.id = :postId
                """, PostStatistics.class)
            .setParameter("postId", postId)
            .getSingleResult();

            assertEquals(postId, postStatistics.getTopic().getId());
        });
    }

    @Entity(name = "Board")
    @Table(name = "board")
    public static class Board {

        @Id
        private Long id;

        private String name;

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
    }

    @MappedSuperclass
    public static abstract class Topic<T extends Topic<T>> {

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

    @MappedSuperclass
    public abstract static class TopicStatistics<T extends Topic> {

        @Id
        private Long id;

        private long views;

        public Long getId() {
            return id;
        }

        public abstract T getTopic();

        public long getViews() {
            return views;
        }

        public TopicStatistics incrementViews() {
            this.views++;
            return this;
        }
    }

    @Entity(name = "PostStatistics")
    @Table(name = "post_statistics")
    public static class PostStatistics extends TopicStatistics<Post> {

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        @JoinColumn(name = "id")
        private Post topic;

        @Override
        public Post getTopic() {
            return topic;
        }

        public PostStatistics setTopic(Post topic) {
            this.topic = topic;
            return this;
        }
    }

    @Entity(name = "AnnouncementStatistics")
    @Table(name = "announcement_statistics")
    public static class AnnouncementStatistics extends TopicStatistics<Announcement> {

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        @JoinColumn(name = "id")
        private Announcement topic;

        @Override
        public Announcement getTopic() {
            return topic;
        }

        public AnnouncementStatistics setTopic(Announcement topic) {
            this.topic = topic;
            return this;
        }
    }
}

