package com.vladmihalcea.book.hpjp.hibernate.query.hierarchical;

import org.hibernate.transform.ResultTransformer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentTreeTransformer implements ResultTransformer {

    public static final PostCommentTreeTransformer INSTANCE = new PostCommentTreeTransformer();

    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        PostComment comment = (PostComment) tuple[0];
        if (comment.getParent() != null) {
            comment.getParent().addChild(comment);
        }
        return comment;
    }

    @Override
    public List transformList(List collection) {
        List<PostComment> comments = (List<PostComment>) collection;
        return comments
                .stream()
                .filter(comment -> comment.getParent() == null)
                .collect(Collectors.toList());
    }
}
