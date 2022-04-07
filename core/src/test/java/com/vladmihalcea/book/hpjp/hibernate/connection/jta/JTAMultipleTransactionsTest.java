package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import net.ttddyy.dsproxy.proxy.ConnectionProxyLogic;
import net.ttddyy.dsproxy.proxy.jdk.ConnectionInvocationHandler;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.XAConnection;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JTAMultipleTransactionsConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JTAMultipleTransactionsTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManagerFactory extraEntityManagerFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    protected final ExecutorService executorService = Executors.newFixedThreadPool(3);

    @Test
    public void testSameJTATransactionMultipleSessionsSameConnection() {
        try(Session session1 = entityManagerFactory.unwrap(SessionFactory.class).openSession()) {
            session1.beginTransaction();

            session1.persist(new FlexyPoolEntities.Post());

            assertEquals(
                1,
                (session1.createQuery(
                    "select count(p) from Post p", Number.class
                ).getSingleResult()).intValue()
            );

            Connection connection1 = session1.doReturningWork((Connection connection) -> connection);

            try(Session session2 = entityManagerFactory.unwrap(SessionFactory.class).openSession()) {
                session2.beginTransaction();

                session2.persist(new FlexyPoolEntities.Post());

                assertEquals(
                    2,
                    (session2.createQuery(
                        "select count(p) from Post p", Number.class
                    ).getSingleResult()).intValue()
                );

                Connection connection2 = session2.doReturningWork((Connection connection) -> connection);

                assertSame(unproxyXAConnection(connection1), unproxyXAConnection(connection2));

                session2.getTransaction().rollback();
            }

            session1.getTransaction().commit();
        } catch (Exception e) {
            assertTrue(e instanceof RollbackException);
        }

        try(Session session = entityManagerFactory.unwrap(SessionFactory.class).openSession()) {
            assertEquals(
                0,
                (session.createQuery(
                    "select count(p) from Post p", Number.class
                ).getSingleResult()).intValue()
            );
        }
    }

    private XAConnection unproxyXAConnection(Connection connection) {
        ConnectionInvocationHandler invocationHandler1 = ((ConnectionInvocationHandler) Proxy.getInvocationHandler(connection));
        ConnectionProxyLogic connectionProxyLogic = ReflectionUtils.getFieldValue(invocationHandler1, "delegate");
        /*Connection connectionProxy = ReflectionUtils.getFieldValue(connectionProxyLogic, "connection");
        JdbcConnectionHandle jdbcConnectionHandle = ((JdbcConnectionHandle) Proxy.getInvocationHandler(connectionProxy));
        JdbcPooledConnection jdbcPooledConnection = ReflectionUtils.getFieldValue(jdbcConnectionHandle, "jdbcPooledConnection");
        return (XAConnection) jdbcPooledConnection.getXAResource();*/
        throw new UnsupportedOperationException("Not Implemented!");
    }

    @Test
    public void testSameJTATransactionMultipleConnectionsMultipleSessions() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

        try(Session session1 = sessionFactory.openSession()) {
            session1.beginTransaction();

            session1.persist(new FlexyPoolEntities.Post());

            assertEquals(
                1,
                (session1.createQuery(
                    "select count(p) from Post p", Number.class
                ).getSingleResult()).intValue()
            );

            try(Session session2 = extraEntityManagerFactory.unwrap(SessionFactory.class).openSession()) {
                session2.joinTransaction();

                session2.persist(new FlexyPoolEntities.Post());

                assertEquals(
                    1,
                    (session2.createQuery(
                        "select count(p) from Post p", Number.class
                    ).getSingleResult()).intValue()
                );

                session2.getTransaction().rollback();
            }

            session1.getTransaction().commit();
        } catch (Exception e) {
            assertTrue(e instanceof RollbackException);
        }

        try(Session session = sessionFactory.openSession()) {
            assertEquals(
                0,
                (session.createQuery(
                    "select count(p) from Post p", Number.class
                ).getSingleResult()).intValue()
            );
        }
    }
}
