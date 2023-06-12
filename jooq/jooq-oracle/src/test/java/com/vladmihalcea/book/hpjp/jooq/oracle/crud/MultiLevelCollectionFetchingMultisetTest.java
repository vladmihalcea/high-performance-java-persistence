package com.vladmihalcea.book.hpjp.jooq.oracle.crud;

import jakarta.persistence.*;
import org.jooq.Records;
import org.jooq.Result;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.Tables.*;
import static org.jooq.impl.DSL.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class MultiLevelCollectionFetchingMultisetTest extends AbstractJOOQOracleSQLIntegrationTest {

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
            UserVote.class
        };
    }

    @Override
    protected String ddlScript() {
        return "clean_schema.sql";
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
                .setFirstName("Alice")
                .setLastName("Smith");

            User bob = new User()
                .setId(2L)
                .setFirstName("Bob")
                .setLastName("Johnson");

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
                        .setReview(String.format("Comment nr. %d", commentId));

                    for (int j = 0; j < VOTE_COUNT; j++) {
                        comment.addVote(
                            new UserVote()
                                .setId(++voteId)
                                .setScore(Math.random() > 0.5 ? 1 : -1)
                                .setUser(Math.random() > 0.5 ? alice : bob)
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
    public void testMultiset() {
        doInJOOQ(sql -> {
            List<PostRecord> posts = sql
                .select(
                    POST.ID,
                    POST.TITLE,
                    multiset(
                        select(
                            POST_COMMENT.ID,
                            POST_COMMENT.REVIEW,
                            multiset(
                                select(
                                    USER_VOTE.ID,
                                    concat(
                                        BLOG_USER.FIRST_NAME,
                                        space(1),
                                        BLOG_USER.LAST_NAME
                                    ),
                                    USER_VOTE.SCORE
                                )
                                .from(USER_VOTE)
                                .leftOuterJoin(BLOG_USER).on(BLOG_USER.ID.eq(USER_VOTE.USER_ID))
                                .where(USER_VOTE.COMMENT_ID.eq(POST_COMMENT.ID))
                            ).as("votes").convertFrom(r -> r.map(Records.mapping(UserVoteRecord::new)))
                        )
                        .from(POST_COMMENT)
                        .where(POST_COMMENT.POST_ID.eq(POST.ID))
                    ).as("comments").convertFrom(r -> r.map(Records.mapping(CommentRecord::new))),
                    multiset(
                        select(
                            POST_TAG.tag().ID,
                            POST_TAG.tag().NAME
                        )
                        .from(POST_TAG)
                        .where(POST_TAG.POST_ID.eq(POST.ID))
                    ).as("tags").convertFrom(r -> r.map(Records.mapping(TagRecord::new)))
                )
                .from(POST)
                .orderBy(POST.ID.asc())
                .fetch(Records.mapping(PostRecord::new));

            assertEquals(POST_COUNT, posts.size());
            PostRecord post = posts.get(0);
            assertEquals(POST_COMMENT_COUNT, post.comments().size());
            assertEquals(TAG_COUNT, post.tags().size());
            CommentRecord comment = post.comments().get(0);
            assertEquals(VOTE_COUNT, comment.votes().size());
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

        @Column(name = "first_name")
        private String firstName;

        @Column(name = "last_name")
        private String lastName;

        public Long getId() {
            return id;
        }

        public User setId(Long id) {
            this.id = id;
            return this;
        }

        public String getFirstName() {
            return firstName;
        }

        public User setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public String getLastName() {
            return lastName;
        }

        public User setLastName(String lastName) {
            this.lastName = lastName;
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

    public static record FlatPostRecord(
        BigInteger postId,
        String postTitle,
        BigInteger commentId,
        String commentReview,
        BigInteger tagId,
        String tagName,
        BigInteger voteId,
        Integer voteScore,
        String userName
    ) {
    }

    public static record PostRecord(
        BigInteger id,
        String title,
        List<CommentRecord> comments,
        List<TagRecord> tags
        ) {
    }

    public static record CommentRecord(
        BigInteger id,
        String review,
        List<UserVoteRecord> votes) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CommentRecord)) return false;
            CommentRecord that = (CommentRecord) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    public static record TagRecord(
        BigInteger id,
        String name) {
    }

    public static record UserVoteRecord(
        BigInteger id,
        String userName,
        BigInteger userVote) {
    }
}
