package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;

public class TableBatchIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Test
    public void testTableIdentifierGenerator() {
        LOGGER.debug("testTableIdentifierGenerator");
        doInJPA(entityManager -> {
            for (int i = 0; i < 5; i++) {
                entityManager.persist(new Post());
            }
            LOGGER.debug("Flush is triggered at commit-time");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GenericGenerator(name = "table", strategy = "enhanced-table", parameters = {
            @org.hibernate.annotations.Parameter(name = "table_name", value = "sequence_table")
        })
        @GeneratedValue(generator = "table", strategy=GenerationType.TABLE)
        private Long id;
    }

}
