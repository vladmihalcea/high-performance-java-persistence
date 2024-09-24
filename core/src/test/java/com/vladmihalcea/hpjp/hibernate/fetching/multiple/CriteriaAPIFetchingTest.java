package com.vladmihalcea.hpjp.hibernate.fetching.multiple;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaAPIFetchingTest extends AbstractPostgreSQLIntegrationTest {

    public static final int POST_COUNT = 50;
    public static final int POST_COMMENT_COUNT = 20;
    public static final int TAG_COUNT = 10;
    public static final int VOTE_COUNT = 5;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
            Tag.class,
            User.class,
            Company.class,
            UserVote.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            User alice = new User()
                .setId(1L)
                .setName("Alice");

            User bob = new User()
                .setId(2L)
                .setName("Bob");

            entityManager.persist(alice);
            entityManager.persist(bob);

            List<Tag> tags = new ArrayList<>();

            for (long i = 1; i <= TAG_COUNT; i++) {
                Tag tag = new Tag()
                    .setId(i)
                    .setName(String.format("Tag nr. %d", i));

                entityManager.persist(tag);
                tags.add(tag);
            }

            long commentId = 0;
            long voteId = 0;

            for (long postId = 1; postId <= POST_COUNT; postId++) {
                Post post = new Post()
                    .setId(postId)
                    .setTitle(String.format("Post nr. %d", postId));


                for (long i = 0; i < POST_COMMENT_COUNT; i++) {
                    PostComment comment = new PostComment()
                        .setId(++commentId)
                        .setReview("Excellent!");

                    for (int j = 0; j < VOTE_COUNT; j++) {
                        comment.addVote(
                            new UserVote()
                                .setId(++voteId)
                                .setScore(Math.random() > 0.5 ? 1 : -1)
                        );
                    }

                    post.addComment(comment);

                }

                for (int i = 0; i < TAG_COUNT; i++) {
                    post.getTags().add(tags.get(i));
                }

                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testInnerJoin() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<UserVote> query = builder.createQuery(UserVote.class);
            Root<UserVote> userVoteRoot = query.from(UserVote.class);

            Fetch<UserVote, PostComment> postCommentJoin = userVoteRoot.fetch("comment");
            Fetch<PostComment, Post> postJoin = postCommentJoin.fetch("post");
            Fetch<UserVote, User> userJoin = userVoteRoot.fetch("user");
            Fetch<User, Company> companyJoin = userJoin.fetch("company");

            query.where(
                builder.like(
                    ((Join) postJoin).get("title"),
                    "Post nr%"
                )
            );

            List<UserVote> userVotes = entityManager
                .createQuery(query)
                .getResultList();

            assertEquals(0, userVotes.size());
        });
    }

    @Test
    public void testLeftJoin() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<UserVote> query = builder.createQuery(UserVote.class);
            Root<UserVote> userVoteRoot = query.from(UserVote.class);

            Fetch<UserVote, PostComment> postCommentJoin = userVoteRoot.fetch("comment", JoinType.LEFT);
            Fetch<PostComment, Post> postJoin = postCommentJoin.fetch("post", JoinType.LEFT);
            Fetch<UserVote, User> userJoin = userVoteRoot.fetch("user", JoinType.LEFT);
            Fetch<User, Company> companyJoin = userJoin.fetch("company", JoinType.LEFT);

            query.where(
                builder.like(
                    ((Join) postJoin).get("title"),
                    "Post nr%"
                )
            );

            List<UserVote> userVotes = entityManager
                .createQuery(query)
                .getResultList();

            assertEquals(POST_COUNT * POST_COMMENT_COUNT * VOTE_COUNT, userVotes.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

        @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private List<Tag> tags = new ArrayList<>();

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

        public List<Tag> getTags() {
            return tags;
        }

        public void setTags(List<Tag> tags) {
            this.tags = tags;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<UserVote> votes = new ArrayList<>();

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

        public List<UserVote> getVotes() {
            return votes;
        }

        public PostComment addVote(UserVote vote) {
            votes.add(vote);
            vote.setComment(this);
            return this;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public Tag setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Tag setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Entity(name = "User")
    @Table(name = "blog_user")
    public static class User {

        @Id
        private Long id;

        private String name;

        @ManyToOne(fetch = FetchType.LAZY)
        private Company company;

        public Long getId() {
            return id;
        }

        public User setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public User setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Entity(name = "Company")
    @Table(name = "company")
    public static class Company {

        @Id
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public Company setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Company setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Entity(name = "UserVote")
    @Table(name = "user_vote")
    public static class UserVote {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private User user;

        @ManyToOne(fetch = FetchType.LAZY)
        private PostComment comment;

        private int score;

        public Long getId() {
            return id;
        }

        public UserVote setId(Long id) {
            this.id = id;
            return this;
        }

        public User getUser() {
            return user;
        }

        public UserVote setUser(User user) {
            this.user = user;
            return this;
        }

        public PostComment getComment() {
            return comment;
        }

        public UserVote setComment(PostComment comment) {
            this.comment = comment;
            return this;
        }

        public int getScore() {
            return score;
        }

        public UserVote setScore(int score) {
            this.score = score;
            return this;
        }
    }
}
