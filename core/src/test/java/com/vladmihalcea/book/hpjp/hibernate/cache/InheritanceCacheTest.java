package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.*;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.Entity;
import java.util.Date;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class InheritanceCacheTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Announcement.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post1 = new Post();
            post1.setId(1L);
            post1.setTitle("High-Performance Java Persistence");

            entityManager.persist(post1);

            Announcement post2 = new Announcement();
            post2.setId(2L);
            post2.setTitle("High-Performance Hibernate");

            entityManager.persist(post2);
        });
    }

    @Test
    public void testEntityLoad() {

        doInJPA(entityManager -> {
            LOGGER.info("First access");
            entityManager.find(Post.class, 1L);
            entityManager.find(Announcement.class, 2L);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Second access");
            entityManager.find(Post.class, 1L);
            entityManager.find(Announcement.class, 2L);
            entityManager.find(Post.class, 2L);
        });
        doInJPA(entityManager -> {
            LOGGER.info("Third access");
            entityManager.find(Post.class, 2L);
        });
    }

    @Entity(name = "Post")
    @Inheritance
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @Entity(name = "Announcement")
    public static class Announcement extends Post {

        @Temporal(TemporalType.DATE)
        private Date validUntil;

        public Date getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(Date validUntil) {
            this.validUntil = validUntil;
        }
    }
}
