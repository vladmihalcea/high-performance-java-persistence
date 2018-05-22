package com.vladmihalcea.book.hpjp.hibernate.query.function;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class GroupConcatFunctionMetadataBuilderContributorRegisterTest extends GroupConcatFunctionTest {

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
                "group_concat",
                new StandardSQLFunction("group_concat", StandardBasicTypes.STRING)
            );
        }
    }

    @Test
    @Ignore
    public void testGroupConcatJPQLQuery() {
        doInJPA(entityManager -> {
            List<PostSummaryDTO> postSummaries = entityManager.createQuery(
                "select " +
                "   p.id as id, " +
                "   p.title as title, " +
                "   group_concat(t.name) as tags " +
                "from Post p " +
                "left join p.tags t " +
                "group by p.id, p.title")
            .unwrap(Query.class)
            .setResultTransformer(Transformers.aliasToBean(PostSummaryDTO.class))
            .getResultList();

            assertEquals(1, postSummaries.size());
            LOGGER.info("Post tags: {}", postSummaries.get(0).getTags());
        });
    }
}
