package com.vladmihalcea.book.hpjp.hibernate.query.hierarchical;

import org.hibernate.SQLQuery;
import org.junit.Test;

import java.util.List;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class TreeConnectByTest extends AbstractTreeTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }

    @Test
    public void test() {

        List<PostComment> comments = doInJPA(entityManager -> {
            return (List<PostComment>) entityManager.createNativeQuery(
                    "SELECT * " +
                    "FROM PostComment c " +
                    "WHERE c.status = :status " +
                    "CONNECT BY PRIOR c.id = c.parent_id " +
                    "START WITH c.parent_id IS NULL AND lower(c.description) like :token ")
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
