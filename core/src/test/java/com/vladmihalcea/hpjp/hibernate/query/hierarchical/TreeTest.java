package com.vladmihalcea.hpjp.hibernate.query.hierarchical;

import org.hibernate.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
                .getResultList();
        });
        assertEquals(2, comments.size());
    }

}
