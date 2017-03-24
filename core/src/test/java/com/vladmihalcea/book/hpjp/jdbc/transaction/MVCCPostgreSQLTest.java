package com.vladmihalcea.book.hpjp.jdbc.transaction;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.query.Query;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MVCCPostgreSQLTest extends AbstractPostgreSQLIntegrationTest {

    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    private final CountDownLatch bobLatch = new CountDownLatch(1);

    private final CountDownLatch endLatch = new CountDownLatch(1);

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Test
    public void testInsert() {
        super.init();
        LOGGER.info("Test insert");
        doInJPA(entityManager -> {
            LOGGER.info("Current transaction id: {}", transactionId(entityManager));

            Post post = new Post();
            post.setId(1);
            post.setTitle("Alice");
            entityManager.persist(post);
            entityManager.flush();

            executeAsync(() -> {
                doInJPA(_entityManager -> {
                    LOGGER.info("Current transaction id: {}", transactionId(_entityManager));
                    LOGGER.info("Fetched Post: {}", getPost(_entityManager, 1));

                    aliceLatch.countDown();
                    awaitOnLatch(bobLatch);

                    LOGGER.info("Fetched Post: {}", getPost(_entityManager, 1));
                    endLatch.countDown();
                });
            });

            awaitOnLatch(aliceLatch);

            LOGGER.info("Fetched Post: {}", getPost(entityManager, 1));
        });
        bobLatch.countDown();
        awaitOnLatch(endLatch);
    }

    @Test
    public void testUpdate() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1);
            post.setTitle("Alice");
            entityManager.persist(post);
        });
        LOGGER.info("Test update");
        doInJPA(entityManager -> {
            LOGGER.info("Current transaction id: {}", transactionId(entityManager));
            LOGGER.info("Fetched Post: {}", getPost(entityManager, 1));

            executeAsync(() -> {
                doInJPA(_entityManager -> {
                    LOGGER.info("Current transaction id: {}", transactionId(_entityManager));
                    LOGGER.info("Fetched Post: {}", getPost(_entityManager, 1));

                    //changing Post title
                    int rowCount = _entityManager.createNativeQuery(
                        "UPDATE post " +
                        "SET title = :title " +
                        "WHERE id = :id ")
                    .setParameter("title", "Bob")
                    .setParameter("id", 1)
                    .executeUpdate();
                    assertEquals(1, rowCount);

                    LOGGER.info("Fetched Post: {}", getPost(_entityManager, 1));

                    aliceLatch.countDown();

                    awaitOnLatch(bobLatch);
                });
                endLatch.countDown();
            });

            awaitOnLatch(aliceLatch);

            LOGGER.info("Fetched Post: {}", getPost(entityManager, 1));

            bobLatch.countDown();

            awaitOnLatch(endLatch);

            LOGGER.info("Fetched Post: {}", getPost(entityManager, 1));
        });
    }

    @Test
    public void testDelete() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1);
            post.setTitle("Alice");
            entityManager.persist(post);
        });
        LOGGER.info("Test delete");
        doInJPA(entityManager -> {
            LOGGER.info("Current transaction id: {}", transactionId(entityManager));
            LOGGER.info("Fetched Post: {}", getPost(entityManager, 1));

            executeAsync(() -> {
                doInJPA(_entityManager -> {
                    LOGGER.info("Current transaction id: {}", transactionId(_entityManager));
                    LOGGER.info("Fetched Post: {}", getPost(_entityManager, 1));

                    //changing Post title
                    int rowCount = _entityManager.createNativeQuery(
                            "DELETE FROM post " +
                            "WHERE id = :id ")
                    .setParameter("id", 1)
                    .executeUpdate();
                    assertEquals(1, rowCount);

                    LOGGER.info("Fetched Post: {}", getPost(_entityManager, 1));
                    aliceLatch.countDown();
                    awaitOnLatch(bobLatch);
                });
            });

            awaitOnLatch(aliceLatch);
            LOGGER.info("Fetched Post: {}", getPost(entityManager, 1));
            bobLatch.countDown();
        });
    }

    private PostWithXminAndXmax getPost(EntityManager entityManager, Integer id) {
        List<PostWithXminAndXmax> result = (List<PostWithXminAndXmax>) entityManager.createNativeQuery(
            "SELECT " +
            "    id, title, CAST(xmin AS text), CAST(xmax AS text) " +
            "FROM Post " +
            "WHERE id = :id")
        .setParameter("id", id)
        .unwrap(Query.class)
        .setResultTransformer(new AliasToBeanResultTransformer(PostWithXminAndXmax.class))
        .getResultList();
        return !result.isEmpty() ? result.get(0) : null;
    }

    private String transactionId(EntityManager entityManager) {
        return (String) entityManager.createNativeQuery(
            "SELECT CAST(txid_current() AS text) ")
        .getSingleResult();
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Integer id;

        private String title;

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

    public static class PostWithXminAndXmax {

        private Integer id;

        private String title;

        private String xmin;

        private String xmax;

        public Integer getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getXmin() {
            return xmin;
        }

        public String getXmax() {
            return xmax;
        }

        @Override
        public String toString() {
            return "Post{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    ", xmin='" + xmin + '\'' +
                    ", xmax='" + xmax + '\'' +
                    '}';
        }
    }
}
