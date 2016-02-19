package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.dialect.Dialect;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BatchingTest - Test to check the JDBC batch support
 *
 * @author Vlad Mihalcea
 */
public class BatchingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", String.valueOf(batchSize()));
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    @Test
    public void testInsertPosts() {
        LOGGER.info("testInsertPosts");
        insertPosts();
    }

    @Test
    public void testInsertPostsAndComments() {
        LOGGER.info("testInsertPostsAndComments");
        doInJPA(entityManager -> {
            for (int i = 0; i < 3; i++) {
                Post post = new Post(String.format("Post no. %d", i));
                post.addComment(new PostComment("Good"));
                entityManager.persist(post);
            }
        });
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

    @Ignore @Test
    public void testInsertAndUpdate() {
        LOGGER.info("Test batch insert");
        long startNanos = System.nanoTime();
        doInJPA(entityManager -> {
            int batchSize = batchSize();
            for (int i = 0; i < itemsCount(); i++) {
                Post post = new Post(String.format("Post no. %d", i));
                int j = 0;
                post.addComment(new PostComment(
                        String.format("Post comment %d:%d", i, j++)));
                post.addComment(new PostComment(
                        String.format("Post comment %d:%d", i, j++)));
                entityManager.persist(post);
                if (i % batchSize == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        });
        LOGGER.info("{}.testInsert took {} millis",
                getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));

        LOGGER.info("Test batch update");
        startNanos = System.nanoTime();

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                    "select distinct p " +
                            "from Post p " +
                            "join fetch p.comments c", Post.class)
                    .getResultList();

            for (Post post : posts) {
                post.setTitle("Blog " + post.getTitle());
                for (PostComment comment : post.getComments()) {
                    comment.setReview("Review " + comment.getReview());
                }
            }
            entityManager.flush();
        });

        LOGGER.info("{}.testUpdate took {} millis",
                getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    @Ignore @Test
    public void testCascadeDelete() {
        LOGGER.info("Test batch delete with cascade");
        final AtomicReference<Long> startNanos = new AtomicReference<>();
        addDeleteBatchingRows();
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                    "select distinct p " +
                            "from Post p " +
                            "join fetch p.details d " +
                            "join fetch p.comments c", Post.class)
                    .getResultList();
            startNanos.set(System.nanoTime());
            for (Post post : posts) {
                entityManager.remove(post);
            }
        });
        LOGGER.info("{}.testCascadeDelete took {} millis",
                getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos.get()));
    }

    @Ignore @Test
    public void testOrphanRemoval() {
        LOGGER.info("Test batch delete with orphan removal");
        final AtomicReference<Long> startNanos = new AtomicReference<>();
        addDeleteBatchingRows();
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                    "select distinct p " +
                            "from Post p " +
                            "join fetch p.details d " +
                            "join fetch p.comments c", Post.class)
                    .getResultList();
            startNanos.set(System.nanoTime());
            entityManager.flush();
            posts.forEach(post -> {
                for (Iterator<PostComment> commentIterator = post.getComments().iterator(); commentIterator.hasNext(); ) {
                    PostComment comment = commentIterator.next();
                    comment.setPost(null);
                    commentIterator.remove();
                }
            });
            entityManager.flush();
            posts.forEach(entityManager::remove);
        });
        LOGGER.info("{}.testOrphanRemoval took {} millis",
                getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos.get()));
    }

    private void addDeleteBatchingRows() {
        doInJPA(entityManager -> {
            int batchSize = batchSize();
            for (int i = 0; i < itemsCount(); i++) {
                Post post = new Post(String.format("Post no. %d", i));
                int j = 0;
                post.addComment(new PostComment(
                        String.format("Post comment %d:%d", i, j++)));
                post.addComment(new PostComment(
                        String.format("Post comment %d:%d", i, j++)));
                entityManager.persist(post);
                if (i % batchSize == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
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
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String title;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

        @OneToOne(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true, fetch = FetchType.LAZY)
        private PostDetails details;

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

        public List<PostComment> getComments() {
            return comments;
        }

        public PostDetails getDetails() {
            return details;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public PostComment() {}

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
