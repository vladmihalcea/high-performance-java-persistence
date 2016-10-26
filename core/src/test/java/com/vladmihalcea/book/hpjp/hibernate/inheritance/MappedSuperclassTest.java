package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

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

    @Test
    public void test() {
        Topic topic = doInJPA(entityManager -> {
            Board board = new Board();
            board.setId(1L);
            board.setName("Hibernate");

            entityManager.persist(board);

            Post post = new Post();
            post.setId(1L);
            post.setOwner("John Doe");
            post.setTitle("Inheritance");
            post.setContent("Best practices");
            post.setBoard(board);

            entityManager.persist(post);

            Announcement announcement = new Announcement();
            announcement.setId(2L);
            announcement.setOwner("John Doe");
            announcement.setTitle("Release x.y.z.Final");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));
            announcement.setBoard(board);

            entityManager.persist(announcement);

            TopicStatistics postStatistics = new PostStatistics(post);
            postStatistics.incrementViews();
            entityManager.persist(postStatistics);

            TopicStatistics announcementStatistics = new AnnouncementStatistics(announcement);
            announcementStatistics.incrementViews();
            entityManager.persist(announcementStatistics);

            return post;
        });

        doInJPA(entityManager -> {
            Long postId = topic.getId();
            LOGGER.info("Fetch statistics");
            PostStatistics statistics = entityManager
                .createQuery("select s from PostStatistics s join fetch s.topic t where t.id = :postId", PostStatistics.class)
                .setParameter("postId", postId)
                .getSingleResult();
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

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @MappedSuperclass
    public static abstract class Topic {

        @Id
        private Long id;

        private String title;

        private String owner;

        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn = new Date();

        @ManyToOne
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

        public void incrementViews() {
            this.views++;
        }
    }

    @Entity(name = "PostStatistics")
    @Table(name = "post_statistics")
    public static class PostStatistics extends TopicStatistics<Post> {

        @OneToOne
        @JoinColumn(name = "id")
        @MapsId
        private Post topic;

        public PostStatistics() {}

        public PostStatistics(Post topic) {
            this.topic = topic;
        }

        @Override
        public Post getTopic() {
            return topic;
        }
    }

    @Entity(name = "AnnouncementStatistics")
    @Table(name = "announcement_statistics")
    public static class AnnouncementStatistics extends TopicStatistics<Announcement> {

        @OneToOne
        @JoinColumn(name = "id")
        @MapsId
        private Announcement topic;

        public AnnouncementStatistics() {}

        public AnnouncementStatistics(Announcement topic) {
            this.topic = topic;
        }

        @Override
        public Announcement getTopic() {
            return topic;
        }
    }
}

