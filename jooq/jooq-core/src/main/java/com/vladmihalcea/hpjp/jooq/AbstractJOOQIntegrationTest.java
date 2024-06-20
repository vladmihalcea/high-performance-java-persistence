package com.vladmihalcea.hpjp.jooq;

import com.vladmihalcea.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.fail;


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
    protected void beforeInit() {
        try {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        Thread.currentThread().getContextClassLoader().getResource(String.format("%s/%s", ddlFolder(), ddlScript())).openStream()
                    )
                )
            ) {
                List<String> sqlStatements = MultiLineSqlScriptExtractor.INSTANCE.extractCommands(reader, dialect());
                try(Connection connection = dataSource().getConnection();
                    Statement statement = connection.createStatement()) {
                    for (String sqlStatement : sqlStatements) {
                        try {
                            statement.execute(sqlStatement);
                        } catch (SQLException ignore) {
                            LOGGER.error("Script [{}] failed when executing statement [{}]", ddlScript(), sqlStatement);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.hbm2ddl.auto", "validate");
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
