package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;

public class PostgresTableGeneratorTest extends AbstractPostgreSQLIntegrationTest {

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
        @GeneratedValue(generator = "pooled")
        @GenericGenerator(name = "pooled", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = {
                @org.hibernate.annotations.Parameter(name = "value_column_name", value = "sequence_next_hi_value"),
                @org.hibernate.annotations.Parameter(name = "prefer_entity_table_as_segment_value", value = "true"),
                @org.hibernate.annotations.Parameter(name = "optimizer", value = "pooled-lo"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "100")})
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
    }

}
