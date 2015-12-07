package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;

public class AssignedTableBatchIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Test
    public void testIdentityIdentifierGenerator() {
        LOGGER.debug("testIdentityIdentifierGenerator");
        doInJPA(entityManager -> {
                for (int i = 0; i < 5; i++) {
                    entityManager.persist(new Post());
                }
                entityManager.flush();
                return null;

        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Identifiable<Long> {

        @Id
        @GenericGenerator(name = "table", strategy = "com.vladmihalcea.book.hpjp.hibernate.identifier.batch.AssignedTableGenerator",
            parameters = {
                @org.hibernate.annotations.Parameter(name = "table_name", value = "sequence_table")
            })
        @GeneratedValue(generator = "table", strategy=GenerationType.TABLE)
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
    }

}
