package com.vladmihalcea.book.hpjp.hibernate.query.recursive;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.ResultTransformer;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WithRecursiveCTEFetchingTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    /**
     * post
     * ----
     *
     * | id | title  |
     * |----|--------|
     * | 1  | Post 1 |
     *
     * post_comment
     * -------------
     *
     * | id | created_on          | review        | score | parent_id | post_id |
     * |----|---------------------|---------------|-------|-----------|---------|
     * | 1  | 2019-10-13 12:23:05 | Comment 1     |   1   |           | 1       |
     * | 2  | 2019-10-14 13:23:10 | Comment 1.1   |   2   | 1         | 1       |
     * | 3  | 2019-10-14 15:45:15 | Comment 1.2   |   2   | 1         | 1       |
     * | 4  | 2019-10-15 10:15:20 | Comment 1.2.1 |   1   | 3         | 1       |
     * | 5  | 2019-10-13 15:23:25 | Comment 2     |   1   |           | 1       |
     * | 6  | 2019-10-14 11:23:30 | Comment 2.1   |   1   | 5         | 1       |
     * | 7  | 2019-10-14 14:45:35 | Comment 2.2   |   1   | 5         | 1       |
     * | 8  | 2019-10-15 10:15:40 | Comment 3     |   1   |           | 1       |
     * | 9  | 2019-10-16 11:15:45 | Comment 3.1   |  10   | 8         | 1       |
     * | 10 | 2019-10-17 18:30:50 | Comment 3.2   |  -2   | 8         | 1       |
     * | 11 | 2019-10-19 21:43:55 | Comment 4     |  -5   |           | 1       |
     * | 12 | 2019-10-22 23:45:00 | Comment 5     |   0   |           | 1       |
     */
    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            Post post = new Post()
                .setId(1L)
                .setTitle("Post 1");

            PostComment comment1 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 13, 12, 23, 5)))
                .setScore(1)
                .setReview("Comment 1");

            PostComment comment1_1 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 13, 23, 10)))
                .setScore(2)
                .setReview("Comment 1.1")
                .setParent(comment1);

            PostComment comment1_2 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 15, 45, 15)))
                .setScore(2)
                .setParent(comment1)
                .setReview("Comment 1.2");

            PostComment comment1_2_1 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 15, 10, 15, 20)))
                .setScore(1)
                .setReview("Comment 1.2.1")
                .setParent(comment1_2);

            PostComment comment2 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 13, 15, 23, 25)))
                .setScore(1)
                .setReview("Comment 2");

            PostComment comment2_1 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 11, 23, 30)))
                .setScore(1)
                .setReview("Comment 2.1")
                .setParent(comment2);

            PostComment comment2_2 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 14, 14, 45, 35)))
                .setScore(1)
                .setReview("Comment 2.2")
                .setParent(comment2);

            PostComment comment3 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 15, 10, 15, 40)))
                .setScore(1)
                .setReview("Comment 3");

            PostComment comment3_1 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 16, 11, 15, 45)))
                .setScore(10)
                .setReview("Comment 3.1")
                .setParent(comment3);

            PostComment comment3_2 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 17, 18, 30, 50)))
                .setScore(-2)
                .setReview("Comment 3.2")
                .setParent(comment3);

            PostComment comment4 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 19, 21, 43, 55)))
                .setReview("Comment 4")
                .setScore(-5);

            PostComment comment5 = new PostComment()
                .setPost(post)
                .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2019, 10, 22, 23, 45, 0)))
                .setReview("Comment 5");

            entityManager.persist(post);
            entityManager.persist(comment1);
            entityManager.persist(comment1_1);
            entityManager.persist(comment1_2);
            entityManager.persist(comment1_2_1);
            entityManager.persist(comment2);
            entityManager.persist(comment2_1);
            entityManager.persist(comment2_2);
            entityManager.persist(comment3);
            entityManager.persist(comment3_1);
            entityManager.persist(comment3_2);
            entityManager.persist(comment4);
            entityManager.persist(comment5);
        });
    }

    /**
     * Get the top two comment hierarchies ordered by total score.
     *
     * This implementation allows the database to calculate the comment hierarchy score,
     * so we can fetch just the top comment hierarchies.
     *
     * This SQL query combines a WITH RECURSIVE query with several Derives Table subsequent queries.
     *
     * SELECT id, parent_id, review, created_on, score, total_score
     * FROM (
     *     SELECT
     *         id, parent_id, review, created_on, score, total_score,
     *         DENSE_RANK() OVER (ORDER BY total_score DESC) AS ranking
     *     FROM (
     *        SELECT
     *            id, parent_id, review, created_on, score,
     *            SUM(score) OVER (PARTITION BY root_id) AS total_score
     *        FROM (
     *           WITH RECURSIVE post_comment_score(
     *               id, root_id, post_id, parent_id, review, created_on, score) 
     *           AS (
     *               SELECT
     *                   id, id, post_id, parent_id, review, created_on, score
     *               FROM post_comment
     *               WHERE post_id = 1 AND parent_id IS NULL
     *               UNION ALL
     *               SELECT pc.id, pcs.root_id, pc.post_id, pc.parent_id,
     *                   pc.review, pc.created_on, pc.score
     *               FROM post_comment pc
     *               INNER JOIN post_comment_score pcs ON pc.parent_id = pcs.id
     *           )
     *           SELECT id, parent_id, root_id, review, created_on, score
     *           FROM post_comment_score
     *        ) total_score_comment
     *     ) total_score_ranking
     * ) total_score_filtering
     * WHERE ranking <= 3
     * ORDER BY total_score DESC, id ASC
     *
     * | id | parent_id | review        | created_on          | score | total_score |
     * |----|-----------|---------------|---------------------|-------|-------------|
     * | 8  |           | Comment 3     | 2019-10-15 10:15:40 |  1    | 9           |
     * | 9  | 8         | Comment 3.1   | 2019-10-16 11:15:45 | 10    | 9           |
     * | 10 | 8         | Comment 3.2   | 2019-10-17 18:30:50 | -2    | 9           |
     * | 1  |           | Comment 1     | 2019-10-13 12:23:05 |  1    | 6           |
     * | 2  | 1         | Comment 1.1   | 2019-10-14 13:23:10 |  2    | 6           |
     * | 3  | 1         | Comment 1.2   | 2019-10-14 15:45:15 |  2    | 6           |
     * | 4  | 3         | Comment 1.2.1 | 2019-10-15 10:15:20 |  1    | 6           |
     * | 5  |           | Comment 2     | 2019-10-13 15:23:25 |  1    | 3           |
     * | 6  | 5         | Comment 2.1   | 2019-10-14 11:23:30 |  1    | 3           |
     * | 7  | 5         | Comment 2.2   | 2019-10-14 14:45:35 |  1    | 3           |
     */
    @Test
    public void testFetchAndSortUsingRecursiveCTEAndDerivedTables() {
        int ranking = 3;

        doInJPA(entityManager -> {
            entityManager.createNativeQuery("""
                UPDATE performance_schema.setup_instruments
                SET enabled = 'YES', timed = 'YES'
                """)
            .executeUpdate();

            entityManager.createNativeQuery("""
                UPDATE performance_schema.setup_consumers
                SET enabled = 'YES'
                """)
            .executeUpdate();

            List<PostCommentScore> postCommentRoots = entityManager
            .createNativeQuery(
                "SELECT id, parent_id, review, created_on, score, total_score " +
                "FROM ( " +
                "    SELECT " +
                "        id, parent_id, review, created_on, score, total_score, " +
                "        dense_rank() OVER (ORDER BY total_score DESC) AS ranking " +
                "    FROM ( " +
                "       SELECT " +
                "           id, parent_id, review, created_on, score, " +
                "           SUM(score) OVER (PARTITION BY root_id) AS total_score " +
                "       FROM ( " +
                "          WITH RECURSIVE post_comment_score( " +
                "              id, root_id, post_id, parent_id, review, created_on, score)  " +
                "          AS ( " +
                "              SELECT " +
                "                  id, id, post_id, parent_id, review, created_on, score " +
                "              FROM post_comment " +
                "              WHERE post_id = :postId AND parent_id IS NULL " +
                "              UNION ALL " +
                "              SELECT pc.id, pcs.root_id, pc.post_id, pc.parent_id, " +
                "                  pc.review, pc.created_on, pc.score " +
                "              FROM post_comment pc " +
                "              INNER JOIN post_comment_score pcs ON pc.parent_id = pcs.id " +
                "          ) " +
                "          SELECT id, parent_id, root_id, review, created_on, score " +
                "          FROM post_comment_score " +
                "       ) total_score_comment " +
                "    ) total_score_ranking " +
                ") total_score_filtering " +
                "WHERE ranking <= :ranking " +
                "ORDER BY total_score DESC, id ASC", "PostCommentScore")
            .unwrap(NativeQuery.class)
            .setParameter("postId", 1L)
            .setParameter("ranking", ranking)
            .setResultTransformer(new PostCommentScoreResultTransformer())
            .getResultList();

            assertEquals(3, postCommentRoots.size());

            assertEquals(9, postCommentRoots.get(0).getTotalScore());
            assertEquals(6, postCommentRoots.get(1).getTotalScore());
            assertEquals(3, postCommentRoots.get(2).getTotalScore());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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
        private Date createdOn;

        private String review;

        private int score;

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

        public PostComment getParent() {
            return parent;
        }

        public PostComment setParent(PostComment parent) {
            this.parent = parent;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public PostComment setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public int getScore() {
            return score;
        }

        public PostComment setScore(int score) {
            this.score = score;
            return this;
        }
    }

    public static class PostCommentScore {

        private Long id;
        private Long parentId;
        private String review;
        private Date createdOn;
        private long score;

        private List<PostCommentScore> children = new ArrayList<>();

        public PostCommentScore(Number id, Number parentId, String review, Date createdOn, Number score) {
            this.id = id.longValue();
            this.parentId = parentId != null ? parentId.longValue() : null;
            this.review = review;
            this.createdOn = createdOn;
            this.score = score.longValue();
        }

        public PostCommentScore() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
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

        public long getScore() {
            return score;
        }

        public void setScore(long score) {
            this.score = score;
        }

        public long getTotalScore() {
            long total = getScore();
            for (PostCommentScore child : children) {
                total += child.getTotalScore();
            }
            return total;
        }

        public List<PostCommentScore> getChildren() {
            List<PostCommentScore> copy = new ArrayList<>(children);
            copy.sort(Comparator.comparing(PostCommentScore::getCreatedOn));
            return copy;
        }

        public void addChild(PostCommentScore child) {
            children.add(child);
        }
    }

    public static class PostCommentScoreResultTransformer implements ResultTransformer {

        private Map<Long, PostCommentScore> postCommentScoreMap = new HashMap<>();

        private List<PostCommentScore> roots = new ArrayList<>();

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            PostCommentScore commentScore = (PostCommentScore) tuple[0];
            Long parentId = commentScore.getParentId();
            if (parentId == null) {
                roots.add(commentScore);
            } else {
                PostCommentScore parent = postCommentScoreMap.get(parentId);
                if (parent != null) {
                    parent.addChild(commentScore);
                }
            }
            postCommentScoreMap.putIfAbsent(commentScore.getId(), commentScore);
            return commentScore;
        }

        @Override
        public List transformList(List collection) {
            return roots;
        }
    }
}
