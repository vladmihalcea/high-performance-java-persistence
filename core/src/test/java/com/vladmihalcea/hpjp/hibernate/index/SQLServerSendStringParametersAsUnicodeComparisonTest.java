package com.vladmihalcea.hpjp.hibernate.index;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import org.hibernate.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.*;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class SQLServerSendStringParametersAsUnicodeComparisonTest extends AbstractTest {

    private final boolean sendStringParametersAsUnicode;

    public SQLServerSendStringParametersAsUnicodeComparisonTest(boolean sendStringParametersAsUnicode) {
        this.sendStringParametersAsUnicode = sendStringParametersAsUnicode;
    }

    @Parameterized.Parameters
    public static Collection<Boolean[]> parameters() {
        return Arrays.asList(new Boolean[][] {
            { true },
            { false }
        });
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        SQLServerDataSourceProvider dataSourceProvider = new SQLServerDataSourceProvider();
        dataSourceProvider.setSendStringParametersAsUnicode(sendStringParametersAsUnicode);
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

                if(i % 100 == 0) {
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
