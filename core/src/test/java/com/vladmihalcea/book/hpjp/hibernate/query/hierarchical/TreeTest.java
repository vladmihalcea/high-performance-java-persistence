package com.vladmihalcea.book.hpjp.hibernate.query.hierarchical;

import org.hibernate.*;
import org.hibernate.transform.ResultTransformer;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class TreeTest extends AbstractTreeTest {

    @Test
    public void test() {

        List<PostComment> comments = doInJPA(entityManager -> {
            return (List<PostComment>) entityManager
                .unwrap(Session.class)
                .createQuery(
                    "SELECT c " +
                    "FROM PostComment c " +
                    "WHERE c.status = :status")
                .setParameter("status", Status.APPROVED)
                .setResultTransformer(PostCommentTreeTransformer.INSTANCE)
                .list();
        });
        assertEquals(2, comments.size());
    }

    @Test
    public void testRecursion() {
        PostComment comment = doInJPA(entityManager -> {
            PostComment root = entityManager.createQuery("select n from Comment n where n.parent is null", PostComment.class).getSingleResult();
            fetchChildren(root);
            return root;
        });
        fetchChildren(comment);
    }

    public void fetchChildren(PostComment comment) {
        for (PostComment _comment : comment.getChildren()) {
            fetchChildren(_comment);
        }
    }

}
