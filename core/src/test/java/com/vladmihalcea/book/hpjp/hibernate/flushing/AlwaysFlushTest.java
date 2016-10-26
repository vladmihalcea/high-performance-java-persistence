package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.*;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class AlwaysFlushTest extends AbstractTest {

    private static final Logger log = Logger.getLogger(AlwaysFlushTest.class);

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Board.class,
            Post.class,
        };
    }

    @Test
    public void testFlushSQL() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("delete from Post").executeUpdate();
            entityManager.createNativeQuery("delete from Board").executeUpdate();
        });
        doInJPA(entityManager -> {
            log.info("testFlushSQL");

            Board board1 = new Board();
            board1.setName("JPA");
            Board board2 = new Board();
            board2.setName("Hibernate");

            entityManager.persist(board1);
            entityManager.persist(board2);

            Post post1 = new Post("JPA 1");
            post1.setVersion(1);
            post1.setBoard(board1);
            entityManager.persist(post1);

            Post post2 = new Post("Hibernate 1");
            post2.setVersion(2);
            post2.setBoard(board2);
            entityManager.persist(post2);

            Post post3 = new Post("Hibernate 3");
            post3.setVersion(1);
            post3.setBoard(board2);
            entityManager.persist(post3);

            Session session = entityManager.unwrap(Session.class);
            List<ForumCount> result = session.createSQLQuery(
                "SELECT " +
                "   b.name as forum, " +
                "   COUNT (p) as count " +
                "FROM post p " +
                "JOIN board b on b.id = p.board_id " +
                "GROUP BY forum")
            .setFlushMode(FlushMode.ALWAYS)
            .setResultTransformer( Transformers.aliasToBean(ForumCount.class))
            .list();

            assertEquals(result.size(), 2);
        });
    }

    @Test
    public void testSynchronizeSQL() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("delete from Post").executeUpdate();
            entityManager.createNativeQuery("delete from Board").executeUpdate();
        });
        doInJPA(entityManager -> {
            log.info("testFlushSQL");

            Board board1 = new Board();
            board1.setName("JPA");
            Board board2 = new Board();
            board2.setName("Hibernate");

            entityManager.persist(board1);
            entityManager.persist(board2);

            Post post1 = new Post("JPA 1");
            post1.setVersion(1);
            post1.setBoard(board1);
            entityManager.persist(post1);

            Post post2 = new Post("Hibernate 1");
            post2.setVersion(2);
            post2.setBoard(board2);
            entityManager.persist(post2);

            Post post3 = new Post("Hibernate 3");
            post3.setVersion(1);
            post3.setBoard(board2);
            entityManager.persist(post3);

            Session session = entityManager.unwrap(Session.class);
            List<ForumCount> result = session.createSQLQuery(
                    "SELECT " +
                    "   b.name as forum, " +
                    "   COUNT (p) as count " +
                    "FROM post p " +
                    "JOIN board b on b.id = p.board_id " +
                    "GROUP BY forum")
            .addSynchronizedEntityClass(Board.class)
            .addSynchronizedEntityClass(Post.class)
            .setResultTransformer( Transformers.aliasToBean(ForumCount.class))
            .list();

            assertEquals(result.size(), 2);
        });
    }

    public static class ForumCount {

        private String forum;

        private BigInteger count;

        public String getForum() {
            return forum;
        }

        public void setForum(String forum) {
            this.forum = forum;
        }

        public BigInteger getCount() {
            return count;
        }

        public void setCount(BigInteger count) {
            this.count = count;
        }
    }

    @Entity(name = "Board")
    @Table(name = "board")
    public static class Board {

        @Id
        @GeneratedValue
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

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @ManyToOne
        private Board board;

        @Version
        private int version;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

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

        public Board getBoard() {
            return board;
        }

        public void setBoard(Board board) {
            this.board = board;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }
}
