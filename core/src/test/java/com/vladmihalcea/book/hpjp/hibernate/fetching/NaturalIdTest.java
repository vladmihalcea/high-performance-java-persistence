package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.junit.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class NaturalIdTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "jcache");
        return properties;
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java persistence");
            post.setSlug("high-performance-java-persistence");
            entityManager.persist(post);
        });
    }

    @Test
    public void testFindBySimpleNaturalId() {
        doInJPA(entityManager -> {
            String slug = "high-performance-java-persistence";

            Post post = entityManager.unwrap(Session.class)
            .bySimpleNaturalId(Post.class)
            .load(slug);

            printNaturalIdCacheRegionStatistics(Post.class);

            assertNotNull(post);
        });
    }

    @Test
    public void testFindByNaturalId() {
        doInJPA(entityManager -> {
            String slug = "high-performance-java-persistence";

            Post post = entityManager.unwrap(Session.class)
            .byNaturalId(Post.class)
            .using("slug", slug)
            .load();

            assertNotNull(post);
        });
    }

    @Test
    public void testFindPostWithJPQL() {
        doInJPA(entityManager -> {
            String slug = "high-performance-java-persistence";

            Post post = entityManager.createQuery("""
                select p
                from Post p
                where p.slug = :slug
                """, Post.class)
            .setParameter("slug", slug)
            .getSingleResult();

            assertNotNull(post);
        });
    }

    @Test
    public void testFindAllPostsWithJPQL() {
        doInJPA(entityManager -> {
            List<String> slugs = List.of(
                "high-performance-java-persistence"
            );

            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where p.slug in (:slugs)
                """, Post.class)
            .setParameter("slugs", slugs)
            .getResultList();

            assertEquals(1, posts.size());
        });
    }

    @Test
    public void testFindPostWithSQL() {
        doInJPA(entityManager -> {
            String slug = "high-performance-java-persistence";

            Post post = (Post) entityManager.createNativeQuery("""
                SELECT *
                FROM post
                WHERE slug = :slug
                """, Post.class)
            .setParameter("slug", slug)
            .getSingleResult();

            assertNotNull(post);
        });
    }

    @Test
    public void testFindAllPostsWithSQL() {
        doInJPA(entityManager -> {
            List<String> slugs = List.of(
                "high-performance-java-persistence"
            );

            List<Post> posts = entityManager.createNativeQuery("""
                SELECT *
                FROM post
                WHERE slug IN (:slugs)
                """, Post.class)
            .setParameter("slugs", slugs)
            .getResultList();

            assertEquals(1, posts.size());
        });
    }
    
    @Test
    public void testFindWithCriteriaAPI() {
        doInJPA(entityManager -> {
            String slug = "high-performance-java-persistence";

            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Post> criteria = builder.createQuery(Post.class);
            Root<Post> p = criteria.from(Post.class);
            criteria.where(builder.equal(p.get("slug"), slug));
            Post post = entityManager.createQuery(criteria).getSingleResult();

            assertNotNull(post);
        });
    }

    @Test
    public void testGetReferenceByNaturalId() {
        doInJPA(entityManager -> {
            String slug = "high-performance-java-persistence";
            Session session = entityManager.unwrap(Session.class);
            LOGGER.info("Loading a post by natural identifier");
            Post post = session.bySimpleNaturalId(Post.class).getReference(slug);
            LOGGER.info("Proxy is loaded");
            LOGGER.info("Post title is {}", post.getTitle());
            assertNotNull(post);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NaturalIdCache
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @NaturalId
        @Column(nullable = false, unique = true)
        private String slug;

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

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Post post = (Post) o;
            return Objects.equals(slug, post.getSlug());
        }

        @Override
        public int hashCode() {
            return Objects.hash(slug);
        }
    }
}
