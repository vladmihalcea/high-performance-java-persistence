package com.vladmihalcea.book.hpjp.hibernate.flushing;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
    @Ignore
    public void testFlushAutoNativeSQL() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager
                    .createNativeQuery(
                        "select count(*) " +
                        "from post")
                    .getSingleResult()
                ).intValue()
            );

            Post post = new Post("High-Performance Java Persistence");

            entityManager.persist(post);

            int postCount = ((Number)
            entityManager
            .createNativeQuery(
                "select count(*) " +
                "from post")
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoNativeSQLFlushModeAlways() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager
                    .createNativeQuery(
                        "select count(*) " +
                        "from post")
                    .getSingleResult()
                ).intValue()
            );

            Post post = new Post("High-Performance Java Persistence");

            entityManager.persist(post);

            int postCount = ((Number)
            entityManager
            .unwrap(Session.class)
            .createNativeQuery(
                "select count(*) " +
                "from post")
            .setFlushMode(FlushMode.ALWAYS)
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoNativeSQLSynchronizedEntityClass() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager
                    .createNativeQuery(
                        "select count(*) " +
                        "from post")
                    .getSingleResult()
                ).intValue()
            );

            Post post = new Post("High-Performance Java Persistence");

            entityManager.persist(post);

            int postCount = ((Number)
            entityManager
            .unwrap(Session.class)
            .createNativeQuery(
                "select count(*) " +
                "from post")
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
                    entityManager
                    .createNativeQuery(
                        "select count(*) " +
                        "from post")
                    .getSingleResult()
                ).intValue()
            );

            Post post = new Post("High-Performance Java Persistence");

            entityManager.persist(post);

            int postCount = ((Number)
            entityManager
            .unwrap(Session.class)
            .createNativeQuery(
                "select count(*) " +
                "from post")
            .addSynchronizedQuerySpace("post")
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }
}
