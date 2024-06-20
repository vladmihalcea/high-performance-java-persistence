package com.vladmihalcea.hpjp.jooq.pgsql.score.transformer;

import com.vladmihalcea.hpjp.jooq.pgsql.score.dto.PostCommentScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentScoreRootTransformer {

    public static final PostCommentScoreRootTransformer INSTANCE = new PostCommentScoreRootTransformer();

    public List<PostCommentScore> transform(List<PostCommentScore> postCommentScores) {
        Map<Long, PostCommentScore> postCommentScoreMap = new HashMap<>();
        List<PostCommentScore> roots = new ArrayList<>();

        for (PostCommentScore postCommentScore : postCommentScores) {
            Long parentId = postCommentScore.getParentId();
            if (parentId == null) {
                roots.add(postCommentScore);
            } else {
                PostCommentScore parent = postCommentScoreMap.get(parentId);
                if (parent != null) {
                    parent.addChild(postCommentScore);
                }
            }
            postCommentScoreMap.putIfAbsent(postCommentScore.getId(), postCommentScore);
        }
        return roots;
    }
}
