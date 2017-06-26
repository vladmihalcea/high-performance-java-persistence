package com.vladmihalcea.book.hpjp.hibernate.identifier.globalsequence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

public class GlobalIdentifierGeneratorScopeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Announcement.class,
        };
    }

    @Override
    protected String[] packages() {
        return new String[] {
            getClass().getPackage().getName()
        };
    }

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return true;
    }

    @Test
    public void testHiloIdentifierGenerator() {
        doInJPA(entityManager -> {
            for(int i = 0; i < 4; i++) {
                Post post = new Post();
                entityManager.persist(post);

                Announcement announcement = new Announcement();
                entityManager.persist(announcement);
            }
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue(generator = "pooled")
        private Long id;
    }

    @Entity(name = "Announcement")
    public static class Announcement {

        @Id
        @GeneratedValue(generator = "pooled")
        private Long id;
    }


}
