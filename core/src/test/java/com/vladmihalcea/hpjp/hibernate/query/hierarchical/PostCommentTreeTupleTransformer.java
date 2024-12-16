package com.vladmihalcea.hpjp.hibernate.query.hierarchical;

import org.hibernate.query.TupleTransformer;
import org.hibernate.transform.ResultTransformer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentTreeTupleTransformer implements TupleTransformer {

    public static final PostCommentTreeTupleTransformer INSTANCE = new PostCommentTreeTupleTransformer();

    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        PostComment comment = (PostComment) tuple[0];
        if (comment.getParent() != null) {
            comment.getParent().addChild(comment);
        }
        return comment.getRoot();
    }
}
