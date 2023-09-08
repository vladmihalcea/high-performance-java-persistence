package com.vladmihalcea.hpjp.spring.data.recursive.repository;

import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.DistinctListTransformer;
import com.vladmihalcea.hpjp.spring.data.recursive.domain.PostCommentDTO;
import jakarta.persistence.EntityManager;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.TupleTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository {

    private final EntityManager entityManager;

    public CustomPostRepositoryImpl(
            EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<PostCommentDTO> findTopCommentDTOsByPost(Long postId, int ranking) {
        return entityManager.createNativeQuery("""
                SELECT id, parent_id, review, created_on, score, total_score
                FROM (
                    SELECT
                        id, parent_id, review, created_on, score, total_score,
                        DENSE_RANK() OVER (ORDER BY total_score DESC) AS ranking
                    FROM (
                       SELECT
                           id, parent_id, review, created_on, score,
                           SUM(score) OVER (PARTITION BY root_id) AS total_score
                       FROM (
                          WITH RECURSIVE post_comment_score(
                              id, root_id, post_id, parent_id, review, created_on, score)
                          AS (
                              SELECT
                                  id, id, post_id, parent_id, review, created_on, score
                              FROM post_comment
                              WHERE post_id = :postId AND parent_id IS NULL
                              UNION ALL
                              SELECT pc.id, pcs.root_id, pc.post_id, pc.parent_id,
                                  pc.review, pc.created_on, pc.score
                              FROM post_comment pc
                              INNER JOIN post_comment_score pcs ON pc.parent_id = pcs.id
                          )
                          SELECT id, parent_id, root_id, review, created_on, score
                          FROM post_comment_score
                       ) total_score_comment
                    ) total_score_ranking
                ) total_score_filtering
                WHERE ranking <= :ranking
                ORDER BY total_score DESC, id ASC
			    """, PostCommentDTO.class.getSimpleName())
            .unwrap(NativeQuery.class)
            .setParameter("postId", 1L)
            .setParameter("ranking", ranking)
            .setTupleTransformer(new PostCommentScoreTupleTransformer())
            .setResultListTransformer(DistinctListTransformer.INSTANCE)
            .getResultList();
    }
    
    public static class PostCommentScoreTupleTransformer implements TupleTransformer {
    
        private Map<Long, PostCommentDTO> postCommentScoreMap = new HashMap<>();

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            PostCommentDTO commentScore = (PostCommentDTO) tuple[0];
            Long parentId = commentScore.getParentId();
            if (parentId != null) {
                PostCommentDTO parent = postCommentScoreMap.get(parentId);
                if (parent != null) {
                    parent.addChild(commentScore);
                }
            }
            postCommentScoreMap.putIfAbsent(commentScore.getId(), commentScore);
            return commentScore.getRoot();
        }
    }
}
