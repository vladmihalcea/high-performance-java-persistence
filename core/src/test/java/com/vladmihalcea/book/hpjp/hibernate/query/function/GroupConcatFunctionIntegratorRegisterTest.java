package com.vladmihalcea.book.hpjp.hibernate.query.function;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.query.Query;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class GroupConcatFunctionIntegratorRegisterTest extends GroupConcatFunctionTest {

    @Override
    protected Integrator integrator() {
        return new Integrator() {
            @Override
            public void integrate(
                    Metadata metadata,
                    SessionFactoryImplementor sessionFactory,
                    SessionFactoryServiceRegistry serviceRegistry) {
                /*metadata.getSqlFunctionMap().put(
                    "group_concat",
                    new StandardSQLFunction( "group_concat", StandardBasicTypes.STRING )
                );*/
            }

            @Override
            public void disintegrate(
                    SessionFactoryImplementor sessionFactory,
                    SessionFactoryServiceRegistry serviceRegistry) {
            }
        };
    }

    @Test
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
