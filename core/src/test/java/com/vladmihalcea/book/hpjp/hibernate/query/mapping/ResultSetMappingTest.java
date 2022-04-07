package com.vladmihalcea.book.hpjp.hibernate.query.mapping;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class ResultSetMappingTest extends AbstractOracleIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "25");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    public static final int POST_COUNT = 50;

    public static final int COMMENT_COUNT = 5;

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            LocalDateTime timestamp = LocalDateTime.of(
                2016, 10, 9, 12, 0, 0, 0
            );

            LongStream.rangeClosed(1, POST_COUNT)
            .forEach(postId -> {
                Post post = new Post()
                .setId(postId)
                .setTitle(
                    String.format(
                        "High-Performance Java Persistence - Chapter %d",
                        postId
                    )
                )
                .setCreatedOn(
                    Timestamp.valueOf(timestamp.plusDays(postId))
                );

                LongStream.rangeClosed(1, COMMENT_COUNT)
                .forEach(commentOffset -> {
                    long commentId = ((postId - 1) * COMMENT_COUNT) + commentOffset;

                    post.addComment(
                        new PostComment()
                        .setId(commentId)
                        .setReview(
                            String.format("Comment nr. %d - A must-read!", commentId)
                        )
                        .setCreatedOn(
                            Timestamp.valueOf(
                                timestamp
                                .plusDays(postId)
                                .plusMinutes(commentId)
                            )
                        )
                    );

                });

                entityManager.persist(post);
            });
        });
    }

    @Test
    public void testEntityResult() {
        doInJPA(entityManager -> {
            final int POST_RESULT_COUNT = 5;

            List<Object[]> postAndCommentList = entityManager
            .createNamedQuery("PostWithCommentByRank")
            .setParameter("titlePattern", "High-Performance Java Persistence %")
            .setParameter("rank", POST_RESULT_COUNT)
            .getResultList();

            assertEquals(POST_RESULT_COUNT * COMMENT_COUNT, postAndCommentList.size());

            for (int i = 0; i < COMMENT_COUNT; i++) {
                Post post = (Post) postAndCommentList.get(i)[0];
                PostComment comment = (PostComment) postAndCommentList.get(i)[1];

                assertTrue(entityManager.contains(post));
                assertTrue(entityManager.contains(comment));

                assertEquals(
                    "High-Performance Java Persistence - Chapter 1",
                    post.getTitle()
                );

                assertEquals(
                    String.format(
                        "Comment nr. %d - A must-read!",
                        i + 1
                    ),
                    comment.getReview()
                );
            }
        });
    }

    @Test
    public void testConstructorResult() {
        doInJPA(entityManager -> {
            final int POST_RESULT_COUNT = 5;

            List<PostTitleWithCommentCount> postTitleAndCommentCountList = entityManager
            .createNamedQuery("PostTitleWithCommentCount")
            .setMaxResults(POST_RESULT_COUNT)
            .getResultList();

            assertEquals(POST_RESULT_COUNT, postTitleAndCommentCountList.size());

            for (int i = 0; i < POST_RESULT_COUNT; i++) {
                PostTitleWithCommentCount postTitleWithCommentCount = postTitleAndCommentCountList.get(i);

                assertEquals(
                    String.format(
                        "High-Performance Java Persistence - Chapter %d",
                        i + 1
                    ),
                    postTitleWithCommentCount.getPostTitle()
                );

                assertEquals(COMMENT_COUNT, postTitleWithCommentCount.getCommentCount());
            }
        });
    }

    @Test
    public void testColumnResult() {
        doInJPA(entityManager -> {
            final int POST_RESULT_COUNT = 5;

            List<Object[]> postWithCommentCountList = entityManager
            .createNamedQuery("PostWithCommentCount")
            .setMaxResults(POST_RESULT_COUNT)
            .getResultList();

            assertEquals(POST_RESULT_COUNT, postWithCommentCountList.size());

            for (int i = 0; i < POST_RESULT_COUNT; i++) {
                Post post = (Post) postWithCommentCountList.get(i)[0];
                int commentCount = (int) postWithCommentCountList.get(i)[1];

                assertTrue(entityManager.contains(post));

                assertEquals(i + 1, post.getId().intValue());
                assertEquals(
                    String.format(
                        "High-Performance Java Persistence - Chapter %d",
                        i + 1
                    ),
                    post.getTitle()
                );

                assertEquals(COMMENT_COUNT, commentCount);
            }
        });
    }
    
    @Entity(name = "Post")
    @Table(name = "post")
    @NamedNativeQuery(
        name = "PostWithCommentByRank",
        query = """
            SELECT *
            FROM (
              SELECT
                *,
                DENSE_RANK() OVER (
                ORDER BY
                  "p.created_on",
                  "p.id"
                ) rank
              FROM (
                SELECT
                  p.id AS "p.id", p.created_on AS "p.created_on",
                  p.title AS "p.title", pc.post_id AS "pc.post_id",
                  pc.id as "pc.id", pc.created_on AS "pc.created_on",
                  pc.review AS "pc.review"
                FROM post p
                LEFT JOIN post_comment pc ON p.id = pc.post_id
                WHERE p.title LIKE :titlePattern
                ORDER BY p.created_on
              ) p_pc
            ) p_pc_r
            WHERE p_pc_r.rank <= :rank
            """,
        resultSetMapping = "PostWithCommentByRankMapping"
    )
    @SqlResultSetMapping(
        name = "PostWithCommentByRankMapping",
        entities = {
            @EntityResult(
                entityClass = Post.class,
                fields = {
                    @FieldResult(name = "id", column = "p.id"),
                    @FieldResult(name = "createdOn", column = "p.created_on"),
                    @FieldResult(name = "title", column = "p.title"),
                }
            ),
            @EntityResult(
                entityClass = PostComment.class,
                fields = {
                    @FieldResult(name = "id", column = "pc.id"),
                    @FieldResult(name = "createdOn", column = "pc.created_on"),
                    @FieldResult(name = "review", column = "pc.review"),
                    @FieldResult(name = "post", column = "pc.post_id"),
                }
            )
        }
    )
    @NamedNativeQuery(
        name = "PostTitleWithCommentCount",
        query = """
            SELECT
              p.id AS "p.id",
              p.title AS "p.title",
              COUNT(pc.*) AS "comment_count"
            FROM post_comment pc
            LEFT JOIN post p ON p.id = pc.post_id
            GROUP BY p.id, p.title
            ORDER BY p.id
            """,
        resultSetMapping = "PostTitleWithCommentCountMapping"
    )
    @SqlResultSetMapping(
        name = "PostTitleWithCommentCountMapping",
        classes = {
            @ConstructorResult(
                columns = {
                    @ColumnResult(name = "p.title"),
                    @ColumnResult(name = "comment_count", type = int.class)
                },
                targetClass = PostTitleWithCommentCount.class
            )
        }
    )
    @NamedNativeQuery(
        name = "PostWithCommentCount",
        query = """
            SELECT
              p.id AS "p.id",
              p.title AS "p.title",
              p.created_on AS "p.created_on",
              COUNT(pc.*) AS "comment_count"
            FROM post_comment pc
            LEFT JOIN post p ON p.id = pc.post_id
            GROUP BY p.id, p.title
            ORDER BY p.id
            """,
        resultSetMapping = "PostWithCommentCountMapping"
    )
    @SqlResultSetMapping(
        name = "PostWithCommentCountMapping",
        entities = @EntityResult(
            entityClass = Post.class,
            fields = {
                @FieldResult(name = "id", column = "p.id"),
                @FieldResult(name = "createdOn", column = "p.created_on"),
                @FieldResult(name = "title", column = "p.title"),
            }
        ),
        columns = @ColumnResult(
            name = "comment_count",
            type = int.class
        )
    )
    public static class Post implements Identifiable<Long> {
    
        @Id
        private Long id;
    
        private String title;
    
        @Column(name = "created_on")
        private Timestamp createdOn;
    
        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
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
    
        public Date getCreatedOn() {
            return createdOn;
        }
    
        public Post setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
            return this;
        }
    
        public List<PostComment> getComments() {
            return comments;
        }
    
        public Post setComments(List<PostComment> comments) {
            this.comments = comments;
            return this;
        }
    
        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    
        public void removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment implements Identifiable<Long> {
    
        @Id
        private Long id;
    
        @ManyToOne
        private Post post;
    
        private String review;
    
        @Column(name = "created_on")
        private Timestamp createdOn;
    
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
    
        public Date getCreatedOn() {
            return createdOn;
        }
    
        public PostComment setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
            return this;
        }
    }

    public static class PostTitleWithCommentCount {

        private final String postTitle;
        private final int commentCount;

        public PostTitleWithCommentCount(
                String postTitle,
                int commentCount) {
            this.postTitle = postTitle;
            this.commentCount = commentCount;
        }

        public String getPostTitle() {
            return postTitle;
        }

        public int getCommentCount() {
            return commentCount;
        }
    }
}
