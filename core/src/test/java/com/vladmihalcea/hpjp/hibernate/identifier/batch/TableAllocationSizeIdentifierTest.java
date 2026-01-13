package com.vladmihalcea.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

public class TableAllocationSizeIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void testTableIdentifierGenerator() {
        LOGGER.debug("testTableIdentifierGenerator");
        int batchSize = 2;
        doInJPA(entityManager -> {
            for (int i = 0; i < batchSize; i++) {
                entityManager.persist(new Post());
            }
            LOGGER.debug("Flush is triggered at commit-time");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @TableGenerator(schema = "sequence_table", name = "table_generator")
        @GeneratedValue(generator = "table_generator", strategy=GenerationType.TABLE)
        private Long id;
    }

}
