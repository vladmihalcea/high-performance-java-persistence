package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.Dialect;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SqlCascadeDeleteBatchingTest - Test to check the SQL cascade delete
 *
 * @author Vlad Mihalcea
 */
public class SqlCascadeDeleteBatchingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostDetails.class,
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
    public void testCascadeDelete() {
        LOGGER.info("Test delete with SQL cascade");
        final AtomicReference<Long> startNanos = new AtomicReference<>();
        addDeleteBatchingRows();
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p from Post p" , Post.class)
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
                post.addDetails(new PostDetails());
                entityManager.persist(post);
                if(i % batchSize == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        });
    }

    protected int itemsCount() {
        return 3;
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

        @Version
        private int version;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        @OnDelete(action = OnDeleteAction.CASCADE)
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

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
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

        public void addDetails(PostDetails details) {
            this.details = details;
            details.setPost(this);
        }

        public void removeDetails() {
            this.details.setPost(null);
            this.details = null;
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @Version
        private int version;

        public PostDetails() {
            createdOn = new Date();
        }

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "id")
        @MapsId
        private Post post;

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

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
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

        @Version
        private int version;

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

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }
}
