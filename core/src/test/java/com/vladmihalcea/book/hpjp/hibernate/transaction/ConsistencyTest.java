package com.vladmihalcea.book.hpjp.hibernate.transaction;

import java.sql.Statement;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;

/**
 * @author Vlad Mihalcea
 */
public class ConsistencyTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Test
    public void test() {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            entityManager.unwrap( Session.class ).doWork( connection -> {
                connection.setAutoCommit( false );
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate( "delete from post" );
                    statement.executeUpdate( "insert into post (title, id) values ('Post nr. 1', 1)" );
                    try{
                        statement.executeUpdate( "insert into post (title, id) values ('Post nr. 1', 1)" );
                    }
                    catch (Exception e) {
                        LOGGER.error( "Constraint", e );
                    }
                    statement.executeUpdate( "insert into post (title, id) values ('Post nr. 2', 2)" );
                    connection.commit();
                }
                catch (Exception e) {
                    connection.rollback();
                }
            } );

        }
        finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        @NaturalId
        private String title;

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

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
    }

}
