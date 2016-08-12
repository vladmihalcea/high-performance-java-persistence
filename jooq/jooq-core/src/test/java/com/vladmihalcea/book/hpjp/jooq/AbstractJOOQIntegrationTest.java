package com.vladmihalcea.book.hpjp.jooq;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
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
        properties.setProperty("hibernate.hbm2ddl.import_files", String.format("%s/%s", ddlFolder(), ddlScript()));
        return properties;
    }

    protected <T> T doInJOOQ(DSLContextCallable<T> callable, Settings settings) {
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory().openSession();
            txn = session.beginTransaction();
            T result = session.doReturningWork(connection -> {
                DSLContext sql = settings != null ?
                    DSL.using(connection, sqlDialect(), settings) :
                    DSL.using(connection, sqlDialect());
                return callable.execute(sql);
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

    protected void doInJOOQ(DSLContextVoidCallable callable, Settings settings) {
        Session session = null;
        Transaction txn = null;
        try {
            session = sessionFactory().openSession();
            txn = session.beginTransaction();
            session.doWork(connection -> {
                DSLContext sql = settings != null ?
                    DSL.using(connection, sqlDialect(), settings) :
                    DSL.using(connection, sqlDialect());
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

    protected <T> T doInJOOQ(DSLContextCallable<T> callable) {
        return doInJOOQ(callable, null);
    }

    protected void doInJOOQ(DSLContextVoidCallable callable) {
        doInJOOQ(callable, null);
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
