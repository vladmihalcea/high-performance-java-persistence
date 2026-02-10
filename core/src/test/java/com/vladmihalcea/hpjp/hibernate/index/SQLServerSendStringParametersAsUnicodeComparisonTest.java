package com.vladmihalcea.hpjp.hibernate.index;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public class SQLServerSendStringParametersAsUnicodeComparisonTest extends AbstractTest {

    private SQLServerDataSourceProvider dataSourceProvider;

    @Parameter
    private boolean sendStringParametersAsUnicode;

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        if (dataSourceProvider == null) {
            dataSourceProvider = new SQLServerDataSourceProvider();
            dataSourceProvider.setSendStringParametersAsUnicode(sendStringParametersAsUnicode);
        }
        return dataSourceProvider;
    }

    public static final int POST_COUNT = 250;

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= POST_COUNT; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format("High-Performance Java Persistence, part %d", i)
                        )
                );

                if (i % 100 == 0) {
                    entityManager.flush();
                }
            }
        });
    }

    @Test
    public void test() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        doInJPA(entityManager -> {
            executeStatement(entityManager, "SET STATISTICS IO, TIME, PROFILE ON");

            LOGGER.info("Test with sendStringParametersAsUnicode=" + sendStringParametersAsUnicode);

            findByTitle(
                entityManager, String.format(
                    "High-Performance Java Persistence, part %d",
                    random.nextLong(POST_COUNT)
                )
            );

            executeStatement(entityManager, "SET STATISTICS IO, TIME, PROFILE OFF");
        });
    }

    @Test
    public void testJpa() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        doInJPA(entityManager -> {
            LOGGER.info("Test with sendStringParametersAsUnicode=" + sendStringParametersAsUnicode);

            findByTitleJPA(
                entityManager, String.format(
                    "High-Performance Java Persistence, part %d",
                    random.nextLong(POST_COUNT)
                )
            );

            List<Tuple> executionPlans = entityManager.createNativeQuery("""
                    SELECT pln.query_plan AS [QueryPlan], dest.text AS [Query], dest.*
                    FROM sys.dm_exec_query_stats AS deqs
                    CROSS APPLY sys.dm_exec_sql_text(deqs.sql_handle) AS dest
                    CROSS APPLY sys.dm_exec_query_plan(deqs.plan_handle) AS pln
                    ORDER BY deqs.last_execution_time DESC
                    """, Tuple.class)
                .getResultList();
            LOGGER.info("Execution plan: {}", executionPlans.get(0).get("QueryPlan"));
        });
    }

    private void findByTitle(EntityManager entityManager, String title) {
        LOGGER.info("Find post by title: {}", title);

        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT PostId, Title
                FROM Post
                WHERE Title = ? 
                """.replaceAll("\n", " ")
            )) {

                statement.setString(1, title);

                if (statement.execute() && statement.getMoreResults()) {
                    LOGGER.info("Execution plan: {}{}",
                        System.lineSeparator(),
                        resultSetToString(statement.getResultSet())
                    );
                }
            }
        });
    }

    private void findByTitleJPA(EntityManager entityManager, String title) {
        LOGGER.info("Find post by title: {}", title);

        List<Long> ids = entityManager.createQuery("""
                select p.id
                from Post p
                where p.title = :title
                """, Long.class)
            .setParameter("title", title)
            .getResultList();
    }

    @Entity(name = "Post")
    @Table(
        name = "Post",
        indexes = @Index(
            name = "IDX_Post_Title",
            columnList = "Title"
        )
    )
    public static class Post {

        @Id
        @Column(name = "PostID")
        private Long id;

        @Column(name = "Title")
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
