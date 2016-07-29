package com.vladmihalcea.book.hpjp.jooq;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.util.Properties;


/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractJOOQIntegrationTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
        };
    }

    protected abstract String ddlFolder();

    protected abstract String ddlScript();

    protected abstract SQLDialect sqlDialect();

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.setProperty("hibernate.hbm2ddl.import_files", String.format("%s/%s.sql", ddlFolder(), ddlScript()));
        return properties;
    }

    protected <T> T doInJOOQ(DSLContextCallable<T> callable) {
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory().openSession();
            txn = session.beginTransaction();
            T result = session.doReturningWork(connection -> {
                DSLContext context = DSL.using(connection, sqlDialect());
                return callable.execute(context);
            });
            txn.commit();
            return result;
        } catch (Throwable e) {
            if ( txn != null ) txn.rollback();
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected void doInJOOQ(DSLContextVoidCallable callable) {
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory().openSession();
            txn = session.beginTransaction();
            session.doWork(connection -> {
                DSLContext sql = DSL.using(connection, sqlDialect());
                callable.execute(sql);
            });
            txn.commit();
        } catch (Throwable e) {
            if ( txn != null ) txn.rollback();
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @FunctionalInterface
    protected interface DSLContextCallable<T> {
        T execute(DSLContext sql) throws SQLException;
    }

    @FunctionalInterface
    protected interface DSLContextVoidCallable {
        void execute(DSLContext sql) throws SQLException;
    }
}
