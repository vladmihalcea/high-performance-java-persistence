package com.vladmihalcea.hpjp.hibernate.query.function;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DateTruncUtcFunctionTest extends AbstractPostgreSQLIntegrationTest {

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
                DateTruncFunction.INSTANCE
            );
        }

        public static class DateTruncFunction extends NamedSqmFunctionDescriptor {

            public static final DateTruncFunction INSTANCE = new DateTruncFunction();

            public DateTruncFunction() {
                super(
                    "date_trunc",
                    false,
                    StandardArgumentsValidators.exactly(1),
                    null
                );
            }

            public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, SqlAstTranslator<?> walker) {
                Expression timestamp = (Expression) arguments.get(0);
                sqlAppender.appendSql("date_trunc('day', (");
                walker.render(timestamp, SqlAstNodeRenderingMode.DEFAULT);
                sqlAppender.appendSql(" AT TIME ZONE 'UTC'))");
            }
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
    @Ignore("Doesn't work on Hibernate 6.2. Workaround needed.")
    public void test() {
        doInJPA(entityManager -> {
            Tuple tuple = entityManager
                .createQuery(
                    "select p.title as title, date_trunc(p.createdOn) as creation_date " +
                    "from Post p " +
                    "where p.id = :postId", Tuple.class)
                .setParameter("postId", 1L)
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
