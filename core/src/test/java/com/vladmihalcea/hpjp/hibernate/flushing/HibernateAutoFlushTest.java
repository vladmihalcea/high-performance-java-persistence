package com.vladmihalcea.hpjp.hibernate.flushing;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HibernateAutoFlushTest extends JPAAutoFlushTest {

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return true;
    }

    @Test
    public void testFlushAutoNativeSQL() {
        doInJPA(entityManager -> {
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
        });
    }

    @Test
    public void testFlushAutoNativeSQLFlushModeAlways() {
        doInJPA(entityManager -> {
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

            assertEquals(
                1,
                ((Number)
                    entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM post
                        """)
                    .unwrap(org.hibernate.query.Query.class)
                    .setHibernateFlushMode(FlushMode.ALWAYS)
                    .getSingleResult()
                ).intValue()
            );
        });
    }

    @Test
    public void testSessionModeAlways() {
        doInJPA(entityManager -> {
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

            entityManager
            .unwrap(Session.class)
            .setHibernateFlushMode(FlushMode.ALWAYS);

            assertEquals(
                1,
                ((Number)
                    entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM post
                        """)
                    .getSingleResult()
                ).intValue()
            );
        });
    }

    @Test
    public void testFlushAutoNativeSQLSynchronizedEntityClass() {
        doInJPA(entityManager -> {
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

            int postCount = ((Number) entityManager.unwrap(Session.class).createNativeQuery("""
                SELECT COUNT(*)
                FROM post
                """)
            .addSynchronizedEntityClass(Post.class)
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoNativeSQLSynchronizedQuerySpace() {
        doInJPA(entityManager -> {
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

            int postCount = ((Number) entityManager.unwrap(Session.class).createNativeQuery("""
                SELECT COUNT(*)
                FROM post
                """)
            .addSynchronizedQuerySpace("post")
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }
}
