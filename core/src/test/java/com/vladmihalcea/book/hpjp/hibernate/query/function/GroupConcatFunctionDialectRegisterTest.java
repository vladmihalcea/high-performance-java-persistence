package com.vladmihalcea.book.hpjp.hibernate.query.function;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.MySQLDataSourceProvider;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Test;

import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class GroupConcatFunctionDialectRegisterTest extends GroupConcatFunctionTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return CustomMySQLDialect.class.getName();
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

    @Test
    public void testGroupConcatCriteriaAPIQuery() {
        doInJPA(entityManager -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();

            CriteriaQuery<PostSummaryDTO> cq = cb.createQuery(PostSummaryDTO.class);
            Root<Post> post = cq.from(Post.class);
            Join tags = post.join("tags", JoinType.LEFT);
            cq.groupBy(post.get("id"), post.get("title"));

            cq.select(
                cb
                .construct(
                    PostSummaryDTO.class,
                    post.get("id"),
                    post.get("title"),
                    cb.function("group_concat", String.class, tags.get("name"))
                )
            );

            List<PostSummaryDTO> postSummaries = entityManager
            .createQuery(cq)
            .getResultList();

            assertEquals(1, postSummaries.size());
            LOGGER.info("Post tags: {}", postSummaries.get(0).getTags());
        });
    }

    public static class CustomMySQLDialect extends MySQL57Dialect {
        public CustomMySQLDialect() {
            super();

            registerFunction(
                    "group_concat",
                    new StandardSQLFunction("group_concat", StandardBasicTypes.STRING)
            );
        }
    }
}
