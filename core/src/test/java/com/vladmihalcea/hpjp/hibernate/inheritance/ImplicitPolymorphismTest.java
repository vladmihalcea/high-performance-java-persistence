package com.vladmihalcea.hpjp.hibernate.inheritance;

import com.vladmihalcea.hpjp.hibernate.type.json.model.*;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ImplicitPolymorphismTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Board.class,
            Post.class,
            Announcement.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void test() {
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
        });
        doInJPA(entityManager -> {
            List<Topic> topics = entityManager.createQuery("""
                select e
                from com.vladmihalcea.hpjp.hibernate.inheritance.ImplicitPolymorphismTest$Topic e
                """)
            .getResultList();

            assertEquals(2, topics.size());
            topics.sort(Comparator.comparing(e -> e.getClass().getName()));

            assertEquals(Announcement.class, topics.get(0).getClass());
            assertEquals(Post.class, topics.get(1).getClass());
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
}
