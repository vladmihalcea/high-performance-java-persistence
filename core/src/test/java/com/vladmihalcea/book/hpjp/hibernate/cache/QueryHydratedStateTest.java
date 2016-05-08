package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class QueryHydratedStateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
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

            Post post2 = new Post();
            post2.setId(2L);
            post2.setTitle("High-Performance Hibernate");

            entityManager.persist(post2);
        });
    }

    @Test
    public void testEntityLoad() {

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.title like :token", Post.class)
            .setParameter("token", "High-Performance%")
            .setHint("org.hibernate.cacheable", true)
            .getResultList();
            assertEquals(1, posts.size());
        });

        doInJPA(entityManager -> {
            LOGGER.info("Load from cache");
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.title like :token", Post.class)
            .setParameter("token", "High-Performance%")
            .setHint("org.hibernate.cacheable", true)
            .getResultList();
            assertEquals(1, posts.size());
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            List<Post> posts = (List<Post>) session.createQuery(
                "select p " +
                "from Post p " +
                "where p.title like :token")
            .setParameter("token", "High-Performance%")
            .setCacheable(true)
            .list();
            assertEquals(1, posts.size());
        });
    }

    @Entity(name = "Post")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
}
