package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.hibernate.query.plan.DefaultInQueryPlanCacheTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class InQueryTest extends AbstractTest {

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
        properties.put("hibernate.query.in_clause_parameter_padding", "true");
    }

    @Test
    public void testPadding() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 15; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle(String.format("Post no. %d", i));

                entityManager.persist(post);
            }
        });

        doInJPA(entityManager -> {
            assertEquals(3, getPostByIds(entityManager, 1, 2, 3).size());
            assertEquals(4, getPostByIds(entityManager, 1, 2, 3, 4).size());
            assertEquals(5, getPostByIds(entityManager, 1, 2, 3, 4, 5).size());
            assertEquals(6, getPostByIds(entityManager, 1, 2, 3, 4, 5, 6).size());
            assertEquals(7, getPostByIds(entityManager, 1, 2, 3, 4, 5, 6, 7).size());
            assertEquals(8, getPostByIds(entityManager, 1, 2, 3, 4, 5, 6, 7, 8).size());
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
        @Column(name = "user_id")
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
