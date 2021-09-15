package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
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
        properties.put("hibernate.cache.region.factory_class", "ehcache");
        return properties;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java persistence")
                    .setSlug("high-performance-java-persistence")
            );
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
    //@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    //@NaturalIdCache
    public static class Post {

        @Id
        private Long id;

        private String title;

        @NaturalId
        @Column(nullable = false, unique = true)
        private String slug;

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

        public String getSlug() {
            return slug;
        }

        public Post setSlug(String slug) {
            this.slug = slug;
            return this;
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
