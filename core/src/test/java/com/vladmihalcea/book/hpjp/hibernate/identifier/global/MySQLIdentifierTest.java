package com.vladmihalcea.book.hpjp.hibernate.identifier.global;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;

public class MySQLIdentifierTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Override
    protected String[] resources() {
        return new String[] {
            "mappings/identifier/global/mysql-orm.xml"
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            for (int i = 0; i < 5; i++) {
                Post post = new Post();
                post.setTitle(String.format("Post nr %d", i + 1));
                entityManager.persist(post);
            }
        });
    }

}
