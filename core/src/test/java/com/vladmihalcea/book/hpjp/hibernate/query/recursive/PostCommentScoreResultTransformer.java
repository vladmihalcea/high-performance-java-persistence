package com.vladmihalcea.book.hpjp.hibernate.query.recursive;

import org.hibernate.transform.ResultTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentScoreResultTransformer implements ResultTransformer {

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
