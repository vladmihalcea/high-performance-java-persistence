package com.vladmihalcea.hpjp.hibernate.flushing;

import org.hibernate.FlushMode;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HibernateAlwaysFlushSessionBuilderTest extends JPAAutoFlushTest {

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return true;
    }

    @Test
    public void testFlushAutoNativeSQL() {
        doInHibernate(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM post
                        """)
                    .getSingleResult()
                ).intValue()
            );

            entityManager.persist(
                new Post()
                .setTitle("High-Performance Java Persistence")
            );

            int postCount = ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM post
                """)
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        }, sessionBuilder -> {
            sessionBuilder.flushMode(FlushMode.ALWAYS);
        });
    }
}
