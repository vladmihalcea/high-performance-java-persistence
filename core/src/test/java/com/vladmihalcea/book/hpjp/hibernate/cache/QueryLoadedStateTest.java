package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class QueryLoadedStateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "jcache");
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());
    }

    public void afterInit() {
        doInJPA(entityManager -> {
            Post post1 = new Post();
            post1.setId(1L);
            post1.setTitle("High-Performance Java Persistence");

            entityManager.persist(post1);

            Post post2 = new Post();
            post2.setId(2L);
            post2.setTitle("High-Performance Hibernate");

            entityManager.persist(post2);
        });
    }

    @Test
    public void test() {

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where p.title like :titlePattern
                """, Post.class)
            .setParameter("titlePattern", "High-Performance%")
            .setHint("org.hibernate.cacheable", true)
            .getResultList();

            assertEquals(2, posts.size());
        });

        printQueryCacheRegionStatistics();

        doInJPA(entityManager -> {
            LOGGER.info("Load from cache");
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where p.title like :titlePattern
                """, Post.class)
            .setParameter("titlePattern", "High-Performance%")
            .setHint("org.hibernate.cacheable", true)
            .getResultList();

            assertEquals(2, posts.size());
        });

        printQueryCacheRegionStatistics();

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where p.title like :titlePattern
                """)
            .setParameter("titlePattern", "High-Performance%")
            .unwrap(org.hibernate.query.Query.class)
            .setCacheable(true)
            .getResultList();

            assertEquals(2, posts.size());
        });

        printQueryCacheRegionStatistics();
    }

    @Entity(name = "Post")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}
