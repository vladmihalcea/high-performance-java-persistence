package com.vladmihalcea.book.hpjp.hibernate.query.function;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DateTruncTimeZoneFunctionTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
                "hibernate.metadata_builder_contributor",
                SqlFunctionsMetadataBuilderContributor.class
        );
    }

    public static class SqlFunctionsMetadataBuilderContributor
            implements MetadataBuilderContributor {

        @Override
        public void contribute(MetadataBuilder metadataBuilder) {
            metadataBuilder.applySqlFunction(
                "date_trunc",
                new StandardSQLFunction("date_trunc('day', (?1 AT TIME ZONE ?2))", false, StandardBasicTypes.TIMESTAMP)
            );
        }
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            post.setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2018, 11, 23, 11, 22, 33)));

            entityManager.persist(post);
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Tuple tuple = entityManager
            .createQuery(
                "select " +
                "   p.title as title, " +
                "   date_trunc(p.createdOn, :timezone) as creation_date " +
                "from " +
                "   Post p " +
                "where " +
                "   p.id = :postId", Tuple.class)
            .setParameter("postId", 1L)
            .setParameter("timezone", "UTC")
            .getSingleResult();

            assertEquals("High-Performance Java Persistence", tuple.get("title"));
            assertEquals(Timestamp.valueOf(LocalDateTime.of(2018, 11, 23, 0, 0, 0)), tuple.get("creation_date"));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private Timestamp createdOn;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Timestamp getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
        }
    }

}
