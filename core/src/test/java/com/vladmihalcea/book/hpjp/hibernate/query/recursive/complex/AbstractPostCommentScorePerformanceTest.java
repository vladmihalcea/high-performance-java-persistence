package com.vladmihalcea.book.hpjp.hibernate.query.recursive.complex;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public abstract class AbstractPostCommentScorePerformanceTest extends AbstractPostgreSQLIntegrationTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    protected com.codahale.metrics.Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
            User.class,
            PostCommentVote.class,
        };
    }

    private User user1;
    private User user2;

    private int postCount;
    private int commentCount;

    public AbstractPostCommentScorePerformanceTest(int postCount, int commentCount) {
        this.postCount = postCount;
        this.commentCount = commentCount;
    }

    @Parameterized.Parameters
    public static Collection<Integer[]> parameters() {
        List<Integer[]> postCountSizes = new ArrayList<>();
        int postCount = 10;
        postCountSizes.add(new Integer[] {postCount, 4});
        postCountSizes.add(new Integer[] {postCount, 4});
        postCountSizes.add(new Integer[] {postCount, 4});
        postCountSizes.add(new Integer[] {postCount, 8});
        postCountSizes.add(new Integer[] {postCount, 16});
        postCountSizes.add(new Integer[] {postCount, 32});
        postCountSizes.add(new Integer[] {postCount, 64});
        return postCountSizes;
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            user1 = new User();
            user1.setUsername("JohnDoe");
            entityManager.persist(user1);

            user2 = new User();
            user2.setUsername("JohnDoeJr");
            entityManager.persist(user2);
        });
        for (long i = 0; i < postCount; i++) {
            insertPost(i);
        }
    }

    private void insertPost(Long postId) {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(postId);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            for (int i = 0; i < commentCount; i++) {
                PostComment comment1 = new PostComment();
                comment1.setPost(post);
                comment1.setReview(String.format("Comment %d", i));
                entityManager.persist(comment1);

                PostCommentVote user1Comment1 = new PostCommentVote(user1, comment1);
                user1Comment1.setUp(entropy());
                entityManager.persist(user1Comment1);

                for (int j = 0; j < commentCount / 2; j++) {
                    PostComment comment1_1 = new PostComment();
                    comment1_1.setParent(comment1);
                    comment1_1.setPost(post);
                    comment1_1.setReview(String.format("Comment %d-%d", i, j));
                    entityManager.persist(comment1_1);

                    PostCommentVote user1Comment1_1 = new PostCommentVote(user1, comment1_1);
                    user1Comment1_1.setUp(entropy());
                    entityManager.persist(user1Comment1_1);

                    PostCommentVote user2Comment1_1 = new PostCommentVote(user2, comment1_1);
                    user2Comment1_1.setUp(entropy());
                    entityManager.persist(user2Comment1_1);

                    for (int k = 0; k < commentCount / 4 ; k++) {
                        PostComment comment1_1_1 = new PostComment();
                        comment1_1_1.setParent(comment1_1_1);
                        comment1_1_1.setPost(post);
                        comment1_1_1.setReview(String.format("Comment %d-%d-%d", i, j, k));
                        entityManager.persist(comment1_1_1);

                        PostCommentVote user1Comment1_1_1 = new PostCommentVote(user1, comment1_1_1);
                        user1Comment1_1_1.setUp(entropy());
                        entityManager.persist(user1Comment1_1_1);

                        PostCommentVote user2Comment1_1_2 = new PostCommentVote(user2, comment1_1_1);
                        user2Comment1_1_2.setUp(entropy());
                        entityManager.persist(user2Comment1_1_2);
                    }
                }
            }
        });
    }

    private boolean entropy() {
        return Math.random() > 0.5d;
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

    @Test
    public void test() {
        int rank = 3;
        for (long postId = 0; postId < postCount; postId++) {
            List<PostCommentScore> result = postCommentScores(postId, rank);
            assertNotNull(result);
        }
        logReporter.report();
    }

    protected abstract List<PostCommentScore> postCommentScores(Long postId, int rank);

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @SqlResultSetMapping(
        name = "PostCommentScore",
        classes = @ConstructorResult(
            targetClass = PostCommentScore.class,
            columns = {
                @ColumnResult(name = "id"),
                @ColumnResult(name = "parent_id"),
                @ColumnResult(name = "root_id"),
                @ColumnResult(name = "review"),
                @ColumnResult(name = "created_on"),
                @ColumnResult(name = "score")
            }
        )
    )
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne
        @JoinColumn(name = "post_id")
        private Post post;

        @ManyToOne
        @JoinColumn(name = "parent_id")
        private PostComment parent;

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name = "created_on")
        private Date createdOn = new Date();

        private String review;

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

        public PostComment getParent() {
            return parent;
        }

        public void setParent(PostComment parent) {
            this.parent = parent;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostComment that = (PostComment) o;
            return Objects.equals(getPost(), that.getPost()) &&
                    Objects.equals(getParent(), that.getParent()) &&
                    Objects.equals(getReview(), that.getReview());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPost(), getReview());
        }

        @Override
        public String toString() {
            return "PostComment{" +
                    "review='" + review + '\'' +
                    ", post=" + post +
                    '}';
        }
    }

    @Entity(name = "User")
    @Table(name = "forum_user")
    public static class User {

        @Id
        @GeneratedValue
        private Long id;

        private String username;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return Objects.equals(getUsername(), user.getUsername());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getUsername());
        }

        @Override
        public String toString() {
            return "User{" +
                    "username='" + username + '\'' +
                    '}';
        }
    }

    @Entity(name = "PostCommentVote")
    @Table(name = "post_comment_vote")
    public static class PostCommentVote implements Serializable {

        @Id
        @ManyToOne
        private User user;

        @Id
        @ManyToOne
        private PostComment comment;

        private boolean up;

        private PostCommentVote() {
        }

        public PostCommentVote(User user, PostComment comment) {
            this.user = user;
            this.comment = comment;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public PostComment getComment() {
            return comment;
        }

        public void setComment(PostComment comment) {
            this.comment = comment;
        }

        public boolean isUp() {
            return up;
        }

        public void setUp(boolean up) {
            this.up = up;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostCommentVote that = (PostCommentVote) o;
            return Objects.equals(getUser(), that.getUser()) &&
                    Objects.equals(getComment(), that.getComment());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getUser(), getComment());
        }

        @Override
        public String toString() {
            return "PostCommentVote{" +
                    "user=" + user +
                    ", comment=" + comment +
                    '}';
        }
    }
}
