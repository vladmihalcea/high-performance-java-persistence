package com.vladmihalcea.book.hpjp.hibernate.query.plan;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PaddingInQueryPlanCacheTest extends AbstractTest {

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
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.generate_statistics", "true");
        properties.put("hibernate.query.in_clause_parameter_padding", "true");
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
            for (int i = 2; i < 16; i++) {
                getPostByIds(
                    entityManager,
                    IntStream.range(1, i).boxed().toArray(Integer[]::new)
                );
            }
            assertEquals(6L, statistics.getQueryPlanCacheMissCount());

            for (String query : statistics.getQueries()) {
                LOGGER.info("Executed query: {}", query);
            }
        });
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
