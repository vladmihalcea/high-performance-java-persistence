package com.vladmihalcea.book.hpjp.jooq.pgsql.score;

import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;
import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScoreResultTransformer;
import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.score.routines.PostCommentScores;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentScoreStoredProcedureTest extends AbstractJOOQPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Override
    public void init() {
        super.init();
        initData();
    }

    protected void initData() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            PostComment comment1 = new PostComment();
            comment1.setPost(post);
            comment1.setReview("Comment 1");
            comment1.setScore(1);
            entityManager.persist(comment1);

            PostComment comment1_1 = new PostComment();
            comment1_1.setParent(comment1);
            comment1_1.setPost(post);
            comment1_1.setReview("Comment 1_1");
            comment1_1.setScore(2);
            entityManager.persist(comment1_1);

            PostComment comment1_2 = new PostComment();
            comment1_2.setParent(comment1);
            comment1_2.setPost(post);
            comment1_2.setReview("Comment 1_2");
            comment1_2.setScore(2);
            entityManager.persist(comment1_2);

            PostComment comment1_2_1 = new PostComment();
            comment1_2_1.setParent(comment1_2);
            comment1_2_1.setPost(post);
            comment1_2_1.setReview("Comment 1_2_1");
            comment1_2_1.setScore(1);
            entityManager.persist(comment1_2_1);

            PostComment comment2 = new PostComment();
            comment2.setPost(post);
            comment2.setReview("Comment 2");
            comment2.setScore(1);
            entityManager.persist(comment2);

            PostComment comment2_1 = new PostComment();
            comment2_1.setParent(comment2);
            comment2_1.setPost(post);
            comment2_1.setReview("Comment 2_1");
            comment2_1.setScore(1);
            entityManager.persist(comment2_1);

            PostComment comment2_2 = new PostComment();
            comment2_2.setParent(comment2);
            comment2_2.setPost(post);
            comment2_2.setReview("Comment 2_2");
            comment2_2.setScore(1);
            entityManager.persist(comment2_2);

            PostComment comment3 = new PostComment();
            comment3.setPost(post);
            comment3.setReview("Comment 3");
            comment3.setScore(1);
            entityManager.persist(comment3);

            PostComment comment3_1 = new PostComment();
            comment3_1.setParent(comment3);
            comment3_1.setPost(post);
            comment3_1.setReview("Comment 3_1");
            comment3_1.setScore(10);
            entityManager.persist(comment3_1);

            PostComment comment3_2 = new PostComment();
            comment3_2.setParent(comment3);
            comment3_2.setPost(post);
            comment3_2.setReview("Comment 3_2");
            comment3_2.setScore(-2);
            entityManager.persist(comment3_2);

            PostComment comment4 = new PostComment();
            comment4.setPost(post);
            comment4.setReview("Comment 4");
            comment4.setScore(-5);
            entityManager.persist(comment4);

            PostComment comment5 = new PostComment();
            comment5.setPost(post);
            comment5.setReview("Comment 5");
            entityManager.persist(comment5);

            entityManager.flush();

        });
    }

    @Test
    public void test() {
        LOGGER.info("Recursive CTE and Window Functions");
        Long postId = 1L;
        int rank = 3;
        List<PostCommentScore> postCommentScoresJPA = postCommentScoresCTEJoin(postId, rank);
        List<PostCommentScore> postCommentScoresJOOQ = PostCommentScoreRootTransformer.INSTANCE.transform(postCommentScoresJOOQ(postId, rank));
        assertEquals(3, postCommentScoresJPA.size());
        assertEquals(3, postCommentScoresJOOQ.size());
    }

    protected List<PostCommentScore> postCommentScoresCTEJoin(Long postId, int rank) {
        return doInJPA(entityManager -> {
            List<PostCommentScore> postCommentScores = entityManager.createNativeQuery(
                "SELECT id, parent_id, review, created_on, score " +
                    "FROM ( " +
                    "    SELECT " +
                    "        id, parent_id, review, created_on, score, " +
                    "        dense_rank() OVER (ORDER BY total_score DESC) rank " +
                    "    FROM ( " +
                    "       SELECT " +
                    "           id, parent_id, review, created_on, score, " +
                    "           SUM(score) OVER (PARTITION BY root_id) total_score " +
                    "       FROM (" +
                    "          WITH RECURSIVE post_comment_score(id, root_id, post_id, " +
                    "              parent_id, review, created_on, score) AS (" +
                    "              SELECT " +
                    "                  id, id, post_id, parent_id, review, created_on, score" +
                    "              FROM post_comment " +
                    "              WHERE post_id = :postId AND parent_id IS NULL " +
                    "              UNION ALL " +
                    "              SELECT pc.id, pcs.root_id, pc.post_id, pc.parent_id, " +
                    "                  pc.review, pc.created_on, pc.score " +
                     "              FROM post_comment pc " +
                    "              INNER JOIN post_comment_score pcs " +
                    "              ON pc.parent_id = pcs.id " +
                    "          ) " +
                    "          SELECT id, parent_id, root_id, review, created_on, score " +
                    "          FROM post_comment_score " +
                    "       ) score_by_comment " +
                    "    ) score_total " +
                    "    ORDER BY total_score DESC, id ASC " +
                    ") total_score_group " +
                    "WHERE rank <= :rank", "PostCommentScore").unwrap(NativeQuery.class)
            .setParameter("postId", postId)
            .setParameter("rank", rank)
            .setResultTransformer(new PostCommentScoreResultTransformer())
            .list();
            return postCommentScores;
        });
    }

    protected List<PostCommentScore> postCommentScoresJOOQ(Long postId, int rank) {
        return doInJOOQ(sql -> {
            PostCommentScores postCommentScores = new PostCommentScores();
            postCommentScores.setPostid(postId);
            postCommentScores.setRankid(rank);
            postCommentScores.execute(sql.configuration());
            return postCommentScores.getReturnValue().into(PostCommentScore.class);
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

        private int score;

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

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }
}
