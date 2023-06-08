package com.vladmihalcea.book.hpjp.jooq.oracle.crud;

import jakarta.persistence.*;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.Tables.*;
import static org.jooq.impl.DSL.concat;
import static org.jooq.impl.DSL.space;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultiLevelCollectionFetchingTest extends AbstractJOOQOracleSQLIntegrationTest {

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
    public void testTwoJoinFetchQueries() {
        List<Post> posts = doInJPA(entityManager -> {
            List<Post> _posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.comments
                where p.id between :minId and :maxId
                """, Post.class)
            .setParameter("minId", 1L)
            .setParameter("maxId", 50L)
            .getResultList();

            entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.tags t
                where p in :posts
                """, Post.class)
            .setParameter("posts", _posts)
            .getResultList();

            entityManager.createQuery("""
                select pc
                from PostComment pc
                left join fetch pc.votes t
                join pc.post p
                where p in :posts
                """, PostComment.class)
            .setParameter("posts", _posts)
            .getResultList();

            return _posts;
        });

        assertEquals(POST_COUNT, posts.size());

        for (Post post : posts) {
            assertEquals(POST_COMMENT_COUNT, post.getComments().size());
            for(PostComment comment : post.getComments()) {
                assertEquals(VOTE_COUNT, comment.getVotes().size());
            }
            assertEquals(TAG_COUNT, post.getTags().size());
        }
    }

    @Test
    public void testCartesianProduct() {
        doInJOOQ(sql -> {
            List<FlatPostRecord> posts = sql
                .select(
                    POST.ID,
                    POST.TITLE,
                    POST_COMMENT.ID,
                    POST_COMMENT.REVIEW,
                    TAG.ID,
                    TAG.NAME,
                    USER_VOTE.ID,
                    USER_VOTE.SCORE,
                    concat(
                        BLOG_USER.FIRST_NAME,
                        space(1),
                        BLOG_USER.LAST_NAME
                    )
                )
                .from(POST)
                .leftOuterJoin(POST_COMMENT).on(POST_COMMENT.POST_ID.eq(POST.ID))
                .leftOuterJoin(POST_TAG).on(POST_TAG.POST_ID.eq(POST.ID))
                .leftOuterJoin(TAG).on(TAG.ID.eq(POST_TAG.TAG_ID))
                .leftOuterJoin(USER_VOTE).on(USER_VOTE.COMMENT_ID.eq(POST_COMMENT.ID))
                .leftOuterJoin(BLOG_USER).on(BLOG_USER.ID.eq(USER_VOTE.USER_ID))
                .orderBy(POST_COMMENT.ID.asc(), POST.ID.asc())
                .fetchInto(FlatPostRecord.class);

            assertEquals(POST_COUNT * POST_COMMENT_COUNT * TAG_COUNT * VOTE_COUNT, posts.size());
        });

        doInJOOQ(sql -> {
            List<PostRecord> posts = sql
                .select(
                    POST.ID,
                    POST.TITLE,
                    POST_COMMENT.ID,
                    POST_COMMENT.REVIEW,
                    TAG.ID,
                    TAG.NAME,
                    USER_VOTE.ID,
                    USER_VOTE.SCORE,
                    concat(
                        BLOG_USER.FIRST_NAME,
                        space(1),
                        BLOG_USER.LAST_NAME
                    )
                )
                .from(POST)
                .leftOuterJoin(POST_COMMENT).on(POST_COMMENT.POST_ID.eq(POST.ID))
                .leftOuterJoin(POST_TAG).on(POST_TAG.POST_ID.eq(POST.ID))
                .leftOuterJoin(TAG).on(TAG.ID.eq(POST_TAG.TAG_ID))
                .leftOuterJoin(USER_VOTE).on(USER_VOTE.COMMENT_ID.eq(POST_COMMENT.ID))
                .leftOuterJoin(BLOG_USER).on(BLOG_USER.ID.eq(USER_VOTE.USER_ID))
                .orderBy(POST_COMMENT.ID.asc(), POST.ID.asc())
                .fetchInto(FlatPostRecord.class)
                .stream().collect(
                    Collectors.collectingAndThen(
                        Collectors.toMap(
                            FlatPostRecord::postId,
                            record -> {
                                PostRecord post = new PostRecord(
                                    record.postId(),
                                    record.postTitle(),
                                    new ArrayList<>(),
                                    new ArrayList<>()
                                );

                                Long commentId = record.commentId();
                                if (commentId != null) {
                                    CommentRecord commentRecord = new CommentRecord(
                                        commentId,
                                        record.commentReview(),
                                        new ArrayList<>()
                                    );

                                    Long voteId = record.voteId();
                                    if (voteId != null) {
                                        commentRecord.votes().add(
                                            new UserVoteRecord(
                                                voteId,
                                                record.userName(),
                                                record.voteScore()
                                            )
                                        );
                                    }
                                    post.comments().add(
                                        commentRecord
                                    );
                                }

                                Long tagId = record.tagId();
                                if (tagId != null) {
                                    post.tags().add(
                                        new TagRecord(
                                            tagId,
                                            record.tagName()
                                        )
                                    );
                                }

                                return post;
                            },
                            (PostRecord existing, PostRecord replacement) -> {
                                if(replacement.comments().size() == 1) {
                                    CommentRecord newCommentRecord = replacement.comments().get(0);
                                    CommentRecord existingCommentRecord = existing.comments().stream().filter(
                                        commentRecord -> commentRecord.id().equals(newCommentRecord.id())
                                    ).findAny().orElse(null);

                                    if(existingCommentRecord == null) {
                                        existing.comments().add(newCommentRecord);
                                    } else {
                                        if(newCommentRecord.votes().size() == 1) {
                                            UserVoteRecord newUserVoteRecord = newCommentRecord.votes().get(0);
                                            if(!existingCommentRecord.votes().contains(newUserVoteRecord)) {
                                                existingCommentRecord.votes().add(newUserVoteRecord);
                                            }
                                        }
                                    }
                                }
                                if(replacement.tags().size() == 1) {
                                    TagRecord newTagRecord = replacement.tags().get(0);
                                    if(!existing.tags().contains(newTagRecord)) {
                                        existing.tags().add(newTagRecord);
                                    }
                                }
                                return existing;
                            },
                            LinkedHashMap::new
                        ),
                        (Function<Map<Long, PostRecord>, List<PostRecord>>) map -> new ArrayList<>(map.values())
                    )
                );

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
        Long postId,
        String postTitle,
        Long commentId,
        String commentReview,
        Long tagId,
        String tagName,
        Long voteId,
        Integer voteScore,
        String userName
    ) {
    }

    public static record PostRecord(
        Long id,
        String title,
        List<CommentRecord> comments,
        List<TagRecord> tags
        ) {
    }

    public static record CommentRecord(
        Long id,
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
        Long id,
        String name) {
    }

    public static record UserVoteRecord(
        Long id,
        String userName,
        Integer userVote) {
    }
}
