package com.vladmihalcea.hpjp.spring.data.jakarta.repository;

import com.vladmihalcea.hpjp.util.ReflectionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.*;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
public class StatelessSessionInvocationHandler implements InvocationHandler {

    private static final List<String> OBJECT_METHODS = Arrays.stream(
        Object.class.getDeclaredMethods()
    ).map(Method::getName).toList();

    private final StatelessSession DUMMY_OBJECT;

    public StatelessSessionInvocationHandler(EntityManagerFactory entityManagerFactory) {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        DUMMY_OBJECT = sessionFactory.withStatelessOptions().openStatelessSession();
        sessionFactory.addObserver(new SessionFactoryObserver() {
            @Override
            public void sessionFactoryClosed(SessionFactory factory) {
                DUMMY_OBJECT.close();
            }
        });
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (OBJECT_METHODS.contains(method.getName())) {
            return ReflectionUtils.invokeMethod(DUMMY_OBJECT, method, args);
        }
        EntityManager entityManager = TransactionSynchronizationManager
            .getResourceMap()
            .values()
            .stream()
            .filter(EntityManagerHolder.class::isInstance)
            .map(eh -> ((EntityManagerHolder) eh).getEntityManager())
            .findAny()
            .orElse(null);

        Session session = entityManager.unwrap(Session.class);
        Connection connection = session.doReturningWork(conn -> conn);
        StatelessSession statelessSession = (StatelessSession) TransactionSynchronizationManager
            .getResource(
                new StatelessSessionKey(entityManager)
            );
        if (statelessSession == null) {
            SessionFactoryImplementor sessionFactory = entityManager
                .getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class);

            final StatelessSession statelessSession_ = sessionFactory.openStatelessSession(connection);
            session.addEventListeners(new BaseSessionEventListener() {
                @Override
                public void end() {
                    statelessSession_.close();
                    TransactionSynchronizationManager.unbindResource(
                        new StatelessSessionKey(entityManager)
                    );
                }
            });

            statelessSession = statelessSession_;
            TransactionSynchronizationManager.bindResource(
                new StatelessSessionKey(entityManager),
                statelessSession
            );
        }

        return ReflectionUtils.invokeMethod(statelessSession, method, args);
    }

    public static class StatelessSessionKey {
        public StatelessSessionKey(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        private final EntityManager entityManager;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StatelessSessionKey that)) return false;
            return Objects.equals(entityManager, that.entityManager);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityManager);
        }
    }
}
