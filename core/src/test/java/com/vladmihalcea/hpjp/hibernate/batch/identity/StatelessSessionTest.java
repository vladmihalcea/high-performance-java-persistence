package com.vladmihalcea.hpjp.hibernate.batch.identity;

import com.vladmihalcea.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.junit.Test;

import java.io.Serializable;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class StatelessSessionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected void beforeInit() {
        executeStatement("drop table if exists post cascade");
        executeStatement("create table post (id bigint not null AUTO_INCREMENT, title varchar(255), primary key (id))");
    }

    @Test
    public void testPersist() {
        StatelessSession session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory().withStatelessOptions().openStatelessSession();
            transaction = session.beginTransaction();
            int i = 1;

            session.setJdbcBatchSize(5);
            
            session.insert(
                new Post().setTitle(
                    String.format(
                        "High-Performance Java Persistence, Part %d",
                        i++
                    )
                )
            );
            session.insert(
                new Post().setTitle(
                    String.format(
                        "High-Performance Java Persistence, Part %d",
                        i++
                    )
                )
            );
            if(transaction != null) {
                transaction.commit();
            }
        } catch (Exception e) {
            LOGGER.error("INSERT failure", e);
            if(transaction != null) {
                transaction.rollback();
            }
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @SQLInsert(sql = "insert into post (id, title) values (default, ?)")
    public static class Post {

        @Id
        @Column(insertable = false)
        @GeneratedValue(generator = "mysql_identity_generator")
        @GenericGenerator(
            name = "mysql_identity_generator",
            strategy = "com.vladmihalcea.hpjp.hibernate.batch.identity.NoIdentityGenerator"
        )
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}
