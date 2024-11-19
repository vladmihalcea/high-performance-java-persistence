package com.vladmihalcea.hpjp.hibernate.logging.comment;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import org.hibernate.LockOptions;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HibernateCommentLoggingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected boolean connectionPooling() {
        return true;
    }

    @Override
    protected int connectionPoolSize() {
        return 1;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.use_sql_comments", "true");
    }

    @Test
    public void testCrud() {
        Post post = doInJPA(entityManager -> {
            Post newPost = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");

            entityManager.persist(newPost);

            return newPost;
        });

        post.setTitle("High-Performance Java Persistence 2nd edition");

        doInJPA(entityManager -> {
            entityManager.merge(post);
        });

        doInJPA(entityManager -> {
            entityManager.remove(
                entityManager.getReference(
                    Post.class,
                    post.getId()
                )
            );
        });
    }

    @Test
    public void testJPQL() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                /* Find post entities matching title pattern */
                select p
                from Post p
                where p.title like :titlePattern
                """, Post.class)
            .setParameter("titlePattern", "High-Performance%")
            .getResultList();

            assertEquals(1, posts.size());
        });
    }
    
    @Test
    public void testCriteriaAPI() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });

        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Post> query = builder.createQuery(Post.class);
            Root<Post> post = query.from(Post.class);
            ParameterExpression<String> titlePattern = builder.parameter(String.class);

            query.where(
                builder.like(post.get(Post_.TITLE), titlePattern)
            );

            List<Post> posts = entityManager.createQuery(query)
                .setParameter(titlePattern, "High-Performance%")
                .getResultList();

            assertEquals(1, posts.size());
        });
    }

    @Test
    public void testCacheMiss() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            entityManager.lock(
                post,
                LockModeType.PESSIMISTIC_READ
            );

            assertEquals("High-Performance Java Persistence", post.getTitle());
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals("High-Performance Java Persistence", post.getTitle());

            entityManager.createNativeQuery("select id from post where id=? for share")
                .setParameter(1, post.getId())
                .getSingleResult();
        });
    }
}
