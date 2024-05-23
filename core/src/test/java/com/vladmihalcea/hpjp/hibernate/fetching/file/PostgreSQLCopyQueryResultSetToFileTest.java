package com.vladmihalcea.hpjp.hibernate.fetching.file;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.PostgreSQLDataSourceProvider;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.AvailableHints;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLCopyQueryResultSetToFileTest extends AbstractTest {

    public static final int BATCH_SIZE = 5000;

    private final String queryResultSetOutputFolder = System.getenv("PG_QUERY_OUTPUT");

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider()
            .setReWriteBatchedInserts(true);
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.STATEMENT_BATCH_SIZE, String.valueOf(BATCH_SIZE));
        properties.setProperty(AvailableSettings.ORDER_INSERTS, Boolean.TRUE.toString());
        properties.setProperty(AvailableSettings.ORDER_UPDATES, Boolean.TRUE.toString());
        properties.setProperty(AvailableSettings.LOG_SLOW_QUERY, "1");
    }

    @Override
    public void afterInit() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        long startNanos = System.nanoTime();
        doInJPA(entityManager -> {
            String[] reviews = new String[] {
                "Excellent book to understand Java Persistence",
                "Must-read for Java developers",
                "Five Stars",
                "A great reference book",
                "The ultimate guide to a critical topic"
            };

            long postCount = 1_000_000;
            //long postCount = 200;
            long commentId = 1;

            for (long id = 1; id <= postCount; id++) {
                Post post = new Post()
                    .setId(id)
                    .setTitle(
                        String.format(
                            "High-Performance Java Persistence - page %d",
                            id
                        )
                    );

                for(String review : reviews) {
                    post.addComment(
                        new PostComment()
                            .setId(commentId++)
                            .setReview(review)
                    );
                }

                entityManager.persist(post);

                if (id % BATCH_SIZE == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        });
        LOGGER.info(
            "Data inserted in {} ms",
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        );
    }

    @Test
    public void test() {
        if (!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        testFetchAll();
        testCopy();
    }

    public void testFetchAll() {
        doInJPA(entityManager -> {
            try {
                String fileName = "post_and_comments.csv";
                Path filePath = Paths.get(queryResultSetOutputFolder, fileName).toAbsolutePath();
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
                assertFalse(Files.exists(filePath));

                long startNanos = System.nanoTime();
                try(Stream<String> tuples = entityManager
                    .createNativeQuery("""
                        SELECT 'post_id,post_title,comment_id,comment_review'
                        UNION ALL
                        SELECT concat_ws(',',
                            p.id,
                            p.title,
                            pc.id,
                            pc.review
                        )
                        FROM post p
                        INNER JOIN post_comment pc ON pc.post_id = p.id
                        """, String.class)
                    .setHint(AvailableHints.HINT_FETCH_SIZE, BATCH_SIZE)
                    .getResultStream()) {
                    Files.write(filePath, (Iterable<String>) tuples::iterator);
                }

                assertTrue(Files.exists(filePath));

                long endNanos = System.nanoTime();
                long lineCount;
                try (Stream<String> stream = Files.lines(filePath)) {
                    lineCount = stream.count();
                }
                LOGGER.info(
                    "Fetched and saved [{}] records in [{}] ms",
                    lineCount - 1,
                    TimeUnit.NANOSECONDS.toMillis(
                        endNanos - startNanos
                    )
                );
                LOGGER.info(
                    "File [{}] size is [{}]",
                    fileName,
                    FileUtils.byteCountToDisplaySize(Files.size(filePath))
                );
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
    }

    public void testCopy() {
        try {
            String fileName = "post_and_comments.csv";
            Path filePath = Paths.get(queryResultSetOutputFolder, fileName).toAbsolutePath();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            assertFalse(Files.exists(filePath));

            long startNanos = System.nanoTime();
            int copyCount = exportQueryResultSet("""
                    SELECT
                        p.id AS post_id,
                        p.title AS post_title,
                        pc.id AS comment_id,
                        pc.review AS comment_review
                    FROM post p
                    INNER JOIN post_comment pc ON pc.post_id = p.id
                """,
                filePath
            );

            LOGGER.info(
                "Copy has exported [{}] records in [{}] ms",
                copyCount,
                TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startNanos
                )
            );

            assertTrue(Files.exists(filePath));
            LOGGER.info(
                "File [{}] size is [{}]",
                fileName,
                FileUtils.byteCountToDisplaySize(Files.size(filePath))
            );
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private int exportQueryResultSet(String query, Path filePath) {
        return doInJPA(entityManager -> {
            return entityManager.createNativeQuery(
                String.format("""
                    COPY (%s)
                    TO '%s'
                    WITH CSV HEADER
                    """,
                    query,
                    filePath
                )
            )
            .executeUpdate();
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

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

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }
}
