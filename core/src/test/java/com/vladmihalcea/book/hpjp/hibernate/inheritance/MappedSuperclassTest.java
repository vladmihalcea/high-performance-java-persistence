package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <code>MappedSuperclassTest</code> - MappedSuperclass Test
 *
 * @author Vlad Mihalcea
 */
public class MappedSuperclassTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Board.class,
            Post.class,
            Announcement.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Board board = new Board();
            board.setId(1L);
            board.setName("Hibernate");

            entityManager.persist(board);

            Post post = new Post();
            post.setId(1L);
            post.setOwner("John Doe");
            post.setTitle("Inheritance best practices");
            post.setContent("Table, Joined and Table per concrete class");
            post.setBoard(board);

            entityManager.persist(post);

            Announcement announcement = new Announcement();
            announcement.setId(2L);
            announcement.setOwner("John Doe");
            announcement.setTitle("Latest release");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));
            announcement.setBoard(board);

            entityManager.persist(announcement);
        });
    }

    @Entity(name = "Board")
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

        private String title;

        private String owner;

        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn = new Date();

        @ManyToOne
        private Board board;

        public abstract Long getId();

        public abstract void setId(Long id);

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

        @Id
        private Long id;

        private String content;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    @Entity(name = "Announcement")
    public static class Announcement extends Topic {

        @Id
        private Long id;

        @Temporal(TemporalType.TIMESTAMP)
        private Date validUntil;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public Date getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(Date validUntil) {
            this.validUntil = validUntil;
        }
    }
}
