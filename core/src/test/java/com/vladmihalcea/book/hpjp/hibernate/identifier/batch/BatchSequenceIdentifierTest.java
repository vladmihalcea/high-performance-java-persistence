package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.book.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.id.BatchSequenceGenerator;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Properties;

public class BatchSequenceIdentifierTest extends AbstractBatchIdentifierTest {

    private static final int POST_SIZE = 10;
    private static final int BATCH_SIZE = 5;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "5");
    }

    @Test
    public void testSequenceIdentifierGenerator() {
        executeStatement("DROP SEQUENCE IF EXISTS post_sequence");
        executeStatement("""
            CREATE SEQUENCE post_sequence
            INCREMENT BY 1
            START WITH 1
            CACHE 5
            """);

        doInJPA(entityManager -> {
            for (int i = 1; i <= POST_SIZE; i++) {
                entityManager.persist(
                    new Post()
                        .setTitle(
                            String.format(
                                "High-Performance Java Persistence, Chapter %d",
                                i
                            )
                        )
                );
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "post_sequence"
        )
        //Uncomment to see the default JPA sequence logic
        /*@SequenceGenerator(name = "post_sequence", allocationSize = 1)*/
        @GenericGenerator(
            name = "post_sequence",
            strategy = "io.hypersistence.utils.hibernate.id.BatchSequenceGenerator",
            parameters = {
                @Parameter(name = "sequence", value = "post_sequence"),
                @Parameter(name = "fetch_size", value = "5")
            }
        )
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}
