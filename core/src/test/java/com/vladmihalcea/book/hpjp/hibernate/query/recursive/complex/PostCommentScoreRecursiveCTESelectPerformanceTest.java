package com.vladmihalcea.book.hpjp.hibernate.query.recursive.complex;

import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;
import org.hibernate.SQLQuery;
import org.hibernate.transform.ResultTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentScoreRecursiveCTESelectPerformanceTest extends AbstractPostCommentScorePerformanceTest {

    public PostCommentScoreRecursiveCTESelectPerformanceTest(int postCount, int commentCount) {
        super(postCount, commentCount);
    }

    @Override
    protected List<PostCommentScore> postCommentScores(Long postId, int rank) {
        return doInJPA(entityManager -> {
            long startNanos = System.nanoTime();
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
                "WHERE rank <= :rank", "PostCommentScore").unwrap(SQLQuery.class)
            .setParameter("postId", postId).setParameter("rank", rank)
            .setResultTransformer(new PostCommentScoreResultTransformer())
            .list();
            timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            return postCommentScores;
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
}
