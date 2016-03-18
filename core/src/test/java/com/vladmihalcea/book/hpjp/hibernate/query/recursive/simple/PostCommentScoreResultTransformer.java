package com.vladmihalcea.book.hpjp.hibernate.query.recursive.simple;

import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;
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
        PostCommentScore postCommentScore = (PostCommentScore) tuple[0];
        if (postCommentScore.getParentId() == null) {
            roots.add(postCommentScore);
        } else {
            PostCommentScore parent = postCommentScoreMap.get(postCommentScore.getParentId());
            if (parent != null) {
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
