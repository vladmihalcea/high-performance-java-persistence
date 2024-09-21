package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.junit.Test;

import java.sql.Connection;
import java.util.concurrent.ExecutionException;

public class HikariConnectionThreadBoundTest extends AbstractTest {

    @Test
    public void test() throws InterruptedException, ExecutionException {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Connection anotherConnection = dataSource().getConnection()) {
                    LOGGER.info("Connections got from RESOURCE_LOCAL transactions are{} bound to thread", connection == anotherConnection ? "" : " not");
                }
            });
        });
    }

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Override
    protected boolean connectionPooling() {
        return true;
    }
}
