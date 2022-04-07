package com.vladmihalcea.book.hpjp.hibernate.statistics;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SlowQueryLogTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            LongStream
            .rangeClosed(1, 50 * 100)
            .forEach(i -> {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format(
                                "High-Performance Java Persistence book - page %d review",
                                i
                            )
                        )
                        .setCreatedBy("Vlad Mihalcea")
                );
                if(i % 50 == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            });
        });
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");

        properties.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "25");
    }

    @Test
    public void testJPQL() {
        LOGGER.info("Check slow JPQL query");

        doInJPA(entityManager -> {
            List<Post> posts = entityManager
                .createQuery("""
                    select p
                    from Post p
                    where lower(title) like :titlePattern
                    order by p.createdOn desc
                """, Post.class)
                .setParameter("titlePattern", "%Java%book%review%".toLowerCase())
                .setFirstResult(1000)
                .setMaxResults(100)
                .getResultList();

            assertEquals(100, posts.size());
        });
    }

    @Test
    public void testCriteriaAPI() {
        LOGGER.info("Check slow Criteria API query");

        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Post> postQuery = builder.createQuery(Post.class);
            Root<Post> post = postQuery.from(Post.class);

            postQuery
                .where(
                    builder.like(builder.lower(post.get("title")), "%Java%book%review%".toLowerCase())
                )
                .orderBy(
                    builder.desc(post.get("createdOn"))
                );

            List<Post> posts = entityManager
                .createQuery(postQuery)
                .setFirstResult(1000)
                .setMaxResults(100)
                .getResultList();

            assertEquals(100, posts.size());

        });
    }

    @Test
    public void testSQL() {
        LOGGER.info("Check slow native SQL query");

        doInJPA(entityManager -> {
            List<Post> posts = entityManager
            .createNativeQuery("""
                SELECT p.*
                FROM post p
                WHERE LOWER(p.title) LIKE :titlePattern
                ORDER BY p.created_on DESC
            """, Post.class)
            .setParameter("titlePattern", "%Java%book%review%".toLowerCase())
            .setFirstResult(1000)
            .setMaxResults(100)
            .getResultList();

            assertEquals(100, posts.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        @CreationTimestamp
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

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

        public Date getCreatedOn() {
            return createdOn;
        }

        public Post setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public Post setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
    }
}
