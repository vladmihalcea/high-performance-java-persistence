package com.vladmihalcea.book.hpjp.hibernate.query.plan;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DefaultInQueryPlanCacheTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.generate_statistics", "true");
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 15; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle(String.format("Post no. %d", i));

                entityManager.persist(post);
            }
        });
    }


    @Test
    public void testInQueryCachePlan() {
        SessionFactory sessionFactory = entityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        doInJPA(entityManager -> {
            for (int i = 1; i < 16; i++) {
                getPostByIds(
                    entityManager,
                    IntStream.range(1, i + 1).boxed().toArray(Integer[]::new)
                );
            }
        });

        assertEquals(16L, statistics.getQueryPlanCacheMissCount());

        for (String query : statistics.getQueries()) {
            LOGGER.info("Executed query: {}", query);
        }
    }

    @Test
    public void testJPQL() {
        SessionFactory sessionFactory = entityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where p.id in :ids
                """, Post.class)
            .setParameter("ids", Arrays.asList(1, 2, 3))
            .getResultList();
        });

        for (String query : statistics.getQueries()) {
            LOGGER.info("Executed query: {}", query);
        }
    }
    
    @Test
    public void testCriteriaAPI() {
        SessionFactory sessionFactory = entityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Post> criteria = builder.createQuery(Post.class);
            Root<Post> fromPost = criteria.from(Post.class);

            criteria.where(builder.in(fromPost.get("id")).value(Arrays.asList(1, 2, 3)));
            List<Post> posts = entityManager.createQuery(criteria).getResultList();
        });

        for (String query : statistics.getQueries()) {
            LOGGER.info("Executed query: {}", query);
        }
    }

    @Test
    public void testSQLQueryCachePlan() {
        SessionFactory sessionFactory = entityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        doInJPA(entityManager -> {
            for (int i = 1; i < 16; i++) {
                List<Post> posts = entityManager.createNativeQuery("""
                    select p.*
                    from post p
                    where p.id = :id
                    """, Post.class)
                .setParameter("id", 1)
                .getResultList();
            }
        });

        assertEquals(1, statistics.getQueryPlanCacheMissCount());

        for (String query : statistics.getQueries()) {
            LOGGER.info("Executed query: {}", query);
        }
    }

    private List<Post> getPostByIds(EntityManager entityManager, Integer... ids) {
        return entityManager.createQuery("""
            select p
            from Post p
            where p.id in :ids
            """, Post.class)
        .setParameter("ids", Arrays.asList(ids))
        .getResultList();
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Integer id;

        private String title;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
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
