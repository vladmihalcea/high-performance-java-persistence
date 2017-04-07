package com.vladmihalcea.book.hpjp.hibernate.query.hierarchical;

import org.hibernate.SQLQuery;
import org.junit.Test;

import java.util.List;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class TreeCTETest extends AbstractTreeTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }

    @Test
    public void test() {

        List<PostComment> comments = doInJPA(entityManager -> {
            return (List<PostComment>) entityManager.createNativeQuery(
                    "WITH RECURSIVE comment_tree(id, parent_id, description, status) AS ( " +
                    "    SELECT c.id, c.parent_id, c.description, status " +
                    "    FROM PostComment c " +
                    "    WHERE LOWER(c.description) LIKE :token AND c.status = :status " +
                    "    UNION ALL " +
                    "    SELECT c.id, c.parent_id, c.description, c.status " +
                    "    FROM PostComment c " +
                    "    INNER JOIN comment_tree ct on ct.id = c.parent_id " +
                    "    WHERE c.status = :status " +
                    ") " +
                    "SELECT id, parent_id, description, status " +
                    "FROM comment_tree ")
                .setParameter("status", Status.APPROVED.name())
                .setParameter("token", "high-performance%")
                .unwrap(SQLQuery.class)
                .addEntity(PostComment.class)
                .setResultTransformer(PostCommentTreeTransformer.INSTANCE)
                .list();
        });
        assertEquals(1, comments.size());
    }
}
