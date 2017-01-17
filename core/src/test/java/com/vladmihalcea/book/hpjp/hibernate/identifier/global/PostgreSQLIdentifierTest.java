package com.vladmihalcea.book.hpjp.hibernate.identifier.global;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

public class PostgreSQLIdentifierTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
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
