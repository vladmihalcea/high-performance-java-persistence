package com.vladmihalcea.book.hpjp.hibernate.transaction;

import java.sql.Statement;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.hibernate.fetching.PostCommentDTO;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;

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
