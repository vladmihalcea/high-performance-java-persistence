package com.vladmihalcea.book.hpjp.hibernate.query.recursive.complex;

import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.ResultTransformer;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Ignore
public class PostCommentScoreTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
            User.class,
            PostCommentVote.class,
        };
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            User user1 = new User();
            user1.setUsername("JohnDoe");
            entityManager.persist(user1);

            User user2 = new User();
            user2.setUsername("JohnDoeJr");
            entityManager.persist(user2);

            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            PostComment comment1 = new PostComment();
            comment1.setPost(post);
            comment1.setReview("Comment 1");
            entityManager.persist(comment1);

            PostCommentVote user1Comment1 = new PostCommentVote(user1, comment1);
            user1Comment1.setUp(true);
            entityManager.persist(user1Comment1);

            PostComment comment1_1 = new PostComment();
            comment1_1.setParent(comment1);
            comment1_1.setPost(post);
            comment1_1.setReview("Comment 1_1");
            entityManager.persist(comment1_1);

            PostCommentVote user1Comment1_1 = new PostCommentVote(user1, comment1_1);
            user1Comment1_1.setUp(true);
            entityManager.persist(user1Comment1_1);

            PostCommentVote user2Comment1_1 = new PostCommentVote(user2, comment1_1);
            user2Comment1_1.setUp(true);
            entityManager.persist(user2Comment1_1);

            PostComment comment1_2 = new PostComment();
            comment1_2.setParent(comment1);
            comment1_2.setPost(post);
            comment1_2.setReview("Comment 1_2");
            entityManager.persist(comment1_2);

            PostCommentVote user1Comment1_2 = new PostCommentVote(user1, comment1_2);
            user1Comment1_2.setUp(true);
            entityManager.persist(user1Comment1_2);

            PostCommentVote user2Comment1_3 = new PostCommentVote(user2, comment1_2);
            user2Comment1_3.setUp(true);
            entityManager.persist(user2Comment1_3);

            PostComment comment1_2_1 = new PostComment();
            comment1_2_1.setParent(comment1_2);
            comment1_2_1.setPost(post);
            comment1_2_1.setReview("Comment 1_2_1");
            entityManager.persist(comment1_2_1);

            PostCommentVote user1Comment1_2_1 = new PostCommentVote(user1, comment1_2_1);
            user1Comment1_2_1.setUp(true);
            entityManager.persist(user1Comment1_2_1);

            PostComment comment2 = new PostComment();
            comment2.setPost(post);
            comment2.setReview("Comment 2");
            entityManager.persist(comment2);

            PostCommentVote user1Comment2 = new PostCommentVote(user1, comment2);
            user1Comment2.setUp(true);
            entityManager.persist(user1Comment2);

            PostComment comment2_1 = new PostComment();
            comment2_1.setParent(comment2);
            comment2_1.setPost(post);
            comment2_1.setReview("Comment 2_1");
            entityManager.persist(comment2_1);

            PostCommentVote user1Comment2_1 = new PostCommentVote(user1, comment2_1);
            user1Comment2_1.setUp(true);
            entityManager.persist(user1Comment2_1);

            PostCommentVote user2Comment2_1 = new PostCommentVote(user2, comment2_1);
            user2Comment2_1.setUp(true);
            entityManager.persist(user2Comment2_1);

            PostComment comment2_2 = new PostComment();
            comment2_2.setParent(comment2);
            comment2_2.setPost(post);
            comment2_2.setReview("Comment 2_2");
            entityManager.persist(comment2_2);

            PostCommentVote user1Comment2_2 = new PostCommentVote(user1, comment2_2);
            user1Comment2_2.setUp(true);
            entityManager.persist(user1Comment2_2);

            PostComment comment3 = new PostComment();
            comment3.setPost(post);
            comment3.setReview("Comment 3");
            entityManager.persist(comment3);

            PostCommentVote user1Comment3 = new PostCommentVote(user1, comment3);
            user1Comment3.setUp(true);
            entityManager.persist(user1Comment3);

            PostComment comment3_1 = new PostComment();
            comment3_1.setParent(comment3);
            comment3_1.setPost(post);
            comment3_1.setReview("Comment 3_1");
            entityManager.persist(comment3_1);

            PostCommentVote user1Comment3_1 = new PostCommentVote(user1, comment3_1);
            user1Comment3_1.setUp(true);
            entityManager.persist(user1Comment3_1);

            PostCommentVote user2Comment3_1 = new PostCommentVote(user2, comment3_1);
            user2Comment3_1.setUp(false);
            entityManager.persist(user2Comment3_1);

            PostComment comment3_2 = new PostComment();
            comment3_2.setParent(comment3);
            comment3_2.setPost(post);
            comment3_2.setReview("Comment 3_2");
            entityManager.persist(comment3_2);

            PostCommentVote user1Comment3_2 = new PostCommentVote(user1, comment3_2);
            user1Comment3_2.setUp(true);
            entityManager.persist(user1Comment3_2);

            PostComment comment4 = new PostComment();
            comment4.setPost(post);
            comment4.setReview("Comment 4");
            entityManager.persist(comment4);

            PostCommentVote user1Comment4 = new PostCommentVote(user1, comment4);
            user1Comment4.setUp(false);
            entityManager.persist(user1Comment4);

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
        List<PostCommentScore> resultCTEJoin = postCommentScoresCTEJoin(postId, rank);
        assertEquals(3, resultCTEJoin.size());

        List<PostCommentScore> resultCTESelect = postCommentScoresCTESelect(postId, rank);
        assertEquals(3, resultCTESelect.size());

        List<PostCommentScore> resultInMemory = postCommentScoresInMemory(postId, rank);
        assertEquals(3, resultInMemory.size());

        for (int i = 0; i < resultCTEJoin.size(); i++) {
            assertEquals(resultCTEJoin.get(i).getTotalScore(), resultInMemory.get(i).getTotalScore());
            assertEquals(resultCTEJoin.get(i).getTotalScore(), resultCTESelect.get(i).getTotalScore());
        }
    }

    private List<PostCommentScore> postCommentScoresCTEJoin(Long postId, int rank) {
        return doInJPA(entityManager -> {
            List<PostCommentScore> postCommentScores = entityManager.createNativeQuery(
                "SELECT id, parent_id, root_id, review, created_on, score " +
                "FROM ( " +
                "    SELECT " +
                "        id, parent_id, root_id, review, created_on, score, " +
                "        dense_rank() OVER (ORDER BY total_score DESC) rank " +
                "    FROM ( " +
                "       SELECT " +
                "           id, parent_id, root_id, review, created_on, score, " +
                "           SUM(score) OVER (PARTITION BY root_id) total_score " +
                "       FROM (" +
                "          WITH RECURSIVE post_comment_score(id, root_id, post_id, " +
                "              parent_id, review, created_on, user_id, score) AS (" +
                "              SELECT id, id, post_id, parent_id, review, created_on, user_id, " +
                "                  CASE WHEN up IS NULL THEN 0 WHEN up = true THEN 1 " +
                "                      ELSE - 1 END score " +
                "              FROM post_comment " +
                "              LEFT JOIN post_comment_vote ON comment_id = id " +
                "              WHERE post_id = :postId AND parent_id IS NULL " +
                "              UNION ALL " +
                "              SELECT distinct pc.id, pcs.root_id, pc.post_id, pc.parent_id, " +
                "                  pc.review, pc.created_on, pcv.user_id, CASE WHEN pcv.up IS NULL THEN 0 " +
                "                  WHEN pcv.up = true THEN 1 ELSE - 1 END score " +
                "              FROM post_comment pc " +
                "              LEFT JOIN post_comment_vote pcv ON pcv.comment_id = pc.id " +
                "              INNER JOIN post_comment_score pcs ON pc.parent_id = pcs.id " +
                "              WHERE pc.parent_id = pcs.id " +
                "          ) " +
                "          SELECT id, parent_id, root_id, review, created_on, SUM(score) score" +
                "          FROM post_comment_score " +
                "          GROUP BY id, parent_id, root_id, review, created_on" +
                "       ) score_by_comment " +
                "    ) score_total " +
                "    ORDER BY total_score DESC, created_on ASC " +
                ") total_score_group " +
                "WHERE rank <= :rank", "PostCommentScore")
            .unwrap(NativeQuery.class)
            .setParameter("postId", postId).setParameter("rank", rank)
            .setResultTransformer(new PostCommentScoreResultTransformer())
            .list();
            return postCommentScores;
        });
    }

    private List<PostCommentScore> postCommentScoresCTESelect(Long postId, int rank) {
        return doInJPA(entityManager -> {
            List<PostCommentScore> postCommentScores = entityManager.createNativeQuery(
                "SELECT id, parent_id, root_id, review, created_on, score " +
                "FROM ( " +
                "    SELECT " +
                "        id, parent_id, root_id, review, created_on, score, " +
                "        dense_rank() OVER (ORDER BY total_score DESC) rank " +
                "    FROM ( " +
                "       SELECT " +
                "           id, parent_id, root_id, review, created_on, score, " +
                "           SUM(score) OVER (PARTITION BY root_id) total_score " +
                "       FROM (" +
                "          WITH RECURSIVE post_comment_score(id, root_id, post_id, " +
                "              parent_id, review, created_on, score) AS (" +
                "              SELECT id, id, post_id, parent_id, review, created_on, " +
                "                COALESCE (( SELECT SUM (CASE WHEN up = true THEN 1 ELSE - 1 END ) FROM post_comment_vote WHERE comment_id = id ), 0)  score " +
                "              FROM post_comment " +
                "              WHERE post_id = :postId AND parent_id IS NULL " +
                "              UNION ALL " +
                "              SELECT pc.id, pcs.root_id, pc.post_id, pc.parent_id, " +
                "                  pc.review, pc.created_on, " +
                "                  COALESCE(( SELECT SUM (CASE WHEN up = true THEN 1 ELSE - 1 END ) FROM post_comment_vote WHERE comment_id = pc.id ), 0)  score " +
                "              FROM post_comment pc " +
                "              INNER JOIN post_comment_score pcs ON pc.parent_id = pcs.id " +
                "              WHERE pc.parent_id = pcs.id " +
                "          ) " +
                "          SELECT id, parent_id, root_id, review, created_on, score" +
                "          FROM post_comment_score" +
                "       ) score_by_comment " +
                "    ) score_total " +
                "    ORDER BY total_score DESC, created_on ASC " +
                ") total_score_group  " +
                "WHERE rank <= :rank", "PostCommentScore")
            .unwrap(NativeQuery.class)
            .setParameter("postId", postId).setParameter("rank", rank)
            .setResultTransformer(new PostCommentScoreResultTransformer())
            .list();
            return postCommentScores;
        });
    }

    protected List<PostCommentScore> postCommentScoresInMemory(Long postId, int rank) {
        return doInJPA(entityManager -> {
            List<PostCommentScore> postCommentScores = entityManager.createQuery(
                "select new com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore(" +
                "   pc.id, pc.parent.id, 0, pc.review, pc.createdOn, sum( case when pcv.up is null then 0 when pcv.up = true then 1 else -1 end ) " +
                ") " +
                "from PostComment pc " +
                "left join PostCommentVote pcv on pc.id = pcv.comment " +
                "where pc.post.id = :postId " +
                "group by pc.id, pc.parent.id, pc.review, pc.createdOn ")
            .setParameter("postId", postId)
            .getResultList();

            Map<Long, List<PostCommentScore>> postCommentScoreMap = postCommentScores.stream().collect(Collectors.groupingBy(PostCommentScore::getId));

            List<PostCommentScore> roots = new ArrayList<>();

            for(PostCommentScore postCommentScore : postCommentScores) {
                Long parentId = postCommentScore.getParentId();
                if(parentId == null) {
                    roots.add(postCommentScore);
                } else {
                    PostCommentScore parent = postCommentScoreMap.get(parentId).get(0);
                    parent.addChild(postCommentScore);
                }
            }

            roots.sort(Comparator.comparing(PostCommentScore::getTotalScore).reversed());

            if(roots.size() > rank) {
                roots = roots.subList(0, rank);
            }
            return  roots;
        });
    }

    public static class PostCommentScoreResultTransformer implements ResultTransformer {

        private Map<Long, PostCommentScore> postCommentScoreMap = new HashMap<>();

        private List<PostCommentScore> roots = new ArrayList<>();

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            PostCommentScore postCommentScore = (PostCommentScore) tuple[0];
            if(postCommentScore.getParentId() == null) {
                roots.add(postCommentScore);
            } else {
                PostCommentScore parent = postCommentScoreMap.get(postCommentScore.getParentId());
                if(parent != null) {
                    parent.addChild(postCommentScore);
                }
            }
            postCommentScoreMap.putIfAbsent(postCommentScore.getId(), postCommentScore);
            return postCommentScore;
        }

        @Override
        public List transformList(List collection) {
            return roots;
        }
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
            return Objects.hash(getPost(), getParent(), getReview());
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
