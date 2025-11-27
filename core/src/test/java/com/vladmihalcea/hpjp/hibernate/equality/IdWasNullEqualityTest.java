package com.vladmihalcea.hpjp.hibernate.equality;

import com.vladmihalcea.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class IdWasNullEqualityTest
        extends AbstractEqualityCheckTest<IdWasNullEqualityTest.Post> {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class,
            PostComment.class
        };
    }

    protected Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.jdbc.batch_size", "100");
        properties.setProperty("hibernate.order_inserts", "true");
        return properties;
    }

    @Test
    public void testEquality() {
        Post post = new Post();
        post.setTitle("High-PerformanceJava Persistence");

        assertEqualityConsistency(Post.class, post);
    }

    @Test
    public void testCollectionSize() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        int collectionSize = 25_000;

        long createSetStartNanos = System.nanoTime();
        Set<Post> postSet = new HashSet<>();

        for (int i = 0; i < collectionSize; i++) {
            Post post = new Post();
            postSet.add(post);
        }

        long createSetEndNanos = System.nanoTime();
        LOGGER.info(
            "Creating a Set with [{}] elements took : [{}] s",
            collectionSize,
            TimeUnit.NANOSECONDS.toSeconds(createSetEndNanos - createSetStartNanos)
        );
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testAddAndFetchCollection() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        int collectionSize = 10000;

        Post post = new Post();
        post.setTitle("High-Performance Java Persistence");
        doInJPA(entityManager -> {
            entityManager.persist(post);

            for (int i = 1; i <= collectionSize; i++) {
                post.addComment(
                    new PostComment()
                        .setReview(String.format("Comment nr. %d", i))
                );
            }
            long createSetStartNanos = System.nanoTime();
            entityManager.flush();
            LOGGER.info(
                "Creating the Set with [{}] elements took : [{}] ms",
                collectionSize,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - createSetStartNanos)
            );
        });

        doInJPA(entityManager -> {
            long fetchSetStartNanos = System.nanoTime();
            Post postWithComments = entityManager.createNamedQuery("POST_WITH_COMMENTS", Post.class)
                .setParameter("postId", post.getId())
                .getSingleResult();
            LOGGER.info(
                "Fetching the Set with [{}] elements took : [{}] ms",
                collectionSize,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - fetchSetStartNanos)
            );
            assertEquals(postWithComments.comments.size(), collectionSize);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @NamedQuery(name = "POST_WITH_COMMENTS", query = """
        select p
        from Post p
        join fetch p.comments
        where p.id = :postId
        """)
    public static class Post implements Identifiable<Long> {

        @Id
        @GeneratedValue
        private Long id;

        private boolean idWasNull;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
        private Set<PostComment> comments = new HashSet<>();

        public Post() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (!(o instanceof Post))
                return false;

            Post other = (Post) o;

            return id != null && id.equals(other.getId());
        }

        @Override
        public int hashCode() {
            Long id = getId();
            if (id == null) idWasNull = true;
            return idWasNull ? 0 : id.hashCode();
        }

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

        public void setTitle(String title) {
            this.title = title;
        }

        public Set<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment implements Identifiable<Long> {

        @Id
        @GeneratedValue
        private Long id;

        private boolean idWasNull;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        public PostComment() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (!(o instanceof PostComment))
                return false;

            PostComment other = (PostComment) o;

            return id != null && id.equals(other.getId());
        }

        @Override
        public int hashCode() {
            Long id = getId();
            if (id == null) idWasNull = true;
            return idWasNull ? 0 : id.hashCode();
        }

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String title) {
            this.review = title;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }
    }
}
