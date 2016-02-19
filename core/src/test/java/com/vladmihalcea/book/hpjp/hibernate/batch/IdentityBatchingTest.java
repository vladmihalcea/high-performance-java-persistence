package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.dialect.Dialect;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;
import java.util.Properties;

/**
 * IdentityBatchingTest - Test to check the identity batch support
 *
 * @author Vlad Mihalcea
 */
public class IdentityBatchingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", String.valueOf(batchSize()));
        return properties;
    }

    @Test
    public void testInsertPosts() {
        LOGGER.info("testInsertPosts");
        insertPosts();
    }

    @Test
    public void testUpdatePosts() {
        insertPosts();

        LOGGER.info("testUpdatePosts");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p ", Post.class)
            .getResultList();

            posts.forEach(post -> post.setTitle(post.getTitle().replaceAll("Post", "Blog post")));
        });
    }

    @Test
    public void testDeletePosts() {
        insertPosts();

        LOGGER.info("testDeletePosts");
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p ", Post.class)
            .getResultList();

            posts.forEach(entityManager::remove);
        });
    }

    private void insertPosts() {
        doInJPA(entityManager -> {
            for (int i = 0; i < 3; i++) {
                entityManager.persist(new Post(String.format("Post no. %d", i + 1)));
            }
        });
    }

    protected int itemsCount() {
        return 5;
    }

    protected int batchSize() {
        return Integer.valueOf(Dialect.DEFAULT_BATCH_SIZE);
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String title;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

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
