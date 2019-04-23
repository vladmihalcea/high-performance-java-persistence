package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertSame;

public class HikariConnectionThreadBoundTest extends AbstractTest {

    @Test
    public void test() throws InterruptedException, ExecutionException {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Connection anotherConnection = dataSource.getConnection()) {
                    LOGGER.info("Connections got from RESOURCE_LOCAL transactions are{} bound to thread", connection == anotherConnection ? "" : " not");
                }
            });
        });

    }

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    private DataSource dataSource;

    protected DataSource newDataSource() {
        dataSource = super.newDataSource();
        return dataSource;
    }

    @Override
    protected boolean connectionPooling() {
        return true;
    }
}
