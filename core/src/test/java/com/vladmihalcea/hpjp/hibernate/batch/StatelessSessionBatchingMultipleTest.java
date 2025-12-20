package com.vladmihalcea.hpjp.hibernate.batch;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.id.SequenceOptimizer;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Root;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaDelete;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * BatchingTest - Test to check the JDBC batch support
 *
 * @author Vlad Mihalcea
 */
public class StatelessSessionBatchingMultipleTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    private static final int POST_COUNT = 3;

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testInsertMultiplePosts() {
        LOGGER.info("testInsertMultiplePosts");

        doInStatelessSession(session -> {
            session.insertMultiple(
                LongStream.rangeClosed(1, POST_COUNT).boxed()
                    .map( i -> new Post()
                        .setId(i)
                        .setTitle(String.format("Post no. %d", i)))
                    .toList()
            );
        });
    }

    @Test
    public void testInsertMultiplePostsAndComments() {
        LOGGER.info("testInsertPostsAndComments");

        doInStatelessSession(session -> {
            List<Post> posts = LongStream.rangeClosed(1, POST_COUNT).boxed()
                .map(i -> new Post()
                    .setId(i)
                    .setTitle(String.format("Post no. %d", i)))
                .toList();

            final int COMMENT_COUNT = 5;

            final List<PostComment> postComments = posts.stream()
                .flatMap(post ->
                    LongStream.rangeClosed(1, COMMENT_COUNT).boxed()
                    .map(i -> new PostComment()
                        .setPost(post)
                        .setReview(
                            String.format(
                                "Post comment no. %d",
                                (post.getId() - 1) * COMMENT_COUNT + i
                            )
                        ))
                ).toList();

            session.insertMultiple(posts);
            session.insertMultiple(postComments);
        });
    }

    @Test
    public void testUpdateMultiplePosts() {
        LOGGER.info("testUpdatePosts");
        insertPosts();

        LOGGER.info("Load Post entities");

        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p
                from Post p   
                """, Post.class)
            .setMaxResults(POST_COUNT)
            .getResultList();
        });

        posts.forEach(post ->
            post.setTitle(post.getTitle().replaceAll("no", "nr"))
        );

        LOGGER.info("Update Post entities");

        doInStatelessSession(session -> {
            session.updateMultiple(posts);
        });
    }

    @Test
    public void testDeleteMultiplePosts() {
        insertPosts();

        LOGGER.info("testDeletePosts");
        doInStatelessSession(session -> {
            List<Post> posts = session.createQuery("""
                select p
                from Post p     
                """, Post.class)
            .getResultList();

            session.deleteMultiple(posts);
        });
    }

    @Test
    public void testDeletePostsAndComments() {
        LOGGER.info("testDeletePostsAndComments");
        insertPostsAndComments();

        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                """, Post.class)
            .setMaxResults(POST_COUNT)
            .getResultList();
        });

        doInStatelessSession(session -> {
            session.deleteMultiple(
                posts.stream()
                    .flatMap(post -> post.getComments().stream())
                    .toList()
            );
            session.deleteMultiple(posts);
        });
    }

    private void insertPosts() {
        doInStatelessSession(session -> {
            for (long i = 1; i <= POST_COUNT; i++) {
                session.insert(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Post no. %d", i))
                );
            }
        });
    }

    private void insertPostsAndComments() {
        doInStatelessSession(session -> {
            List<Post> posts = LongStream.rangeClosed(1, POST_COUNT).boxed()
                .map(i -> {
                    Post post = new Post()
                        .setId(i)
                        .setTitle(String.format("Post no. %d", i));

                    session.insert(post);

                    return post;
                })
                .toList();

            final int COMMENT_COUNT = 5;

            posts.forEach(post -> {
                for (long i = 1; i <= COMMENT_COUNT; i++) {
                    session.insert(
                        new PostComment()
                            .setPost(post)
                            .setReview(
                                String.format(
                                    "Post comment no. %d",
                                    (post.getId() - 1) * COMMENT_COUNT + i
                                )
                            )
                    );
                }
            });
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
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
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @SequenceOptimizer(
            sequenceName = "seq_post_comment",
            optimizer = "pooled-lo"
        )
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
