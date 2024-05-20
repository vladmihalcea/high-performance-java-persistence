package com.vladmihalcea.hpjp.hibernate.batch.identity.stateless;

import com.vladmihalcea.hpjp.hibernate.batch.identity.stateless.model.BatchInsertPost;
import com.vladmihalcea.hpjp.hibernate.batch.identity.stateless.model.Post;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class MySQLIdentityStatelessSessionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            BatchInsertPost.class
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
        executeStatement("create table post (version smallint, created_on datetime(6), id bigint not null AUTO_INCREMENT, updated_on datetime(6), created_by varchar(255), title varchar(255), updated_by varchar(255), primary key (id))"
        );
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
                new BatchInsertPost().setTitle(
                    String.format(
                        "High-Performance Java Persistence, Part %d",
                        i++
                    )
                )
            );
            session.insert(
                new BatchInsertPost().setTitle(
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
}
