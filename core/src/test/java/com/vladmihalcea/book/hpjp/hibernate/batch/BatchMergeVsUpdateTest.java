package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.annotations.QueryHints;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class BatchMergeVsUpdateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "5");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    public void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format("High-Performance Java Persistence, Part no. %d", i)
                        )
                        .addComment(
                            new PostComment()
                                .setReview("Excellent")
                        )
                );
            }
        });
    }

    @Test
    public void testMerge() {
        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select distinct p
                from Post p
                join fetch p.comments
                """, Post.class)
            .getResultList();
        });

        for (Post post : posts) {
            post.setTitle("Vlad Mihalcea's " + post.getTitle());
            for (PostComment comment : post.getComments()) {
                comment.setReview(comment.getReview() + " read!");
            }
        }

        doInJPA(entityManager -> {
            LOGGER.info("Merge");
            for (Post post : posts) {
                entityManager.merge(post);
            }
        });
    }

    @Test
    public void testUpdate() {
        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select distinct p
                from Post p
                join fetch p.comments
                """, Post.class)
            .getResultList();
        });

        for (Post post : posts) {
            post.setTitle("Vlad Mihalcea's " + post.getTitle());
            for (PostComment comment : post.getComments()) {
                comment.setReview(comment.getReview() + " read!");
            }
        }

        doInJPA(entityManager -> {
            LOGGER.info("Update");
            Session session = entityManager.unwrap(Session.class);
            for (Post post : posts) {
                session.update(post);
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
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
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }
}
