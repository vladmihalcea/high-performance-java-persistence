package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class NativeQueryTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }

    @Test
    public void testPagination() {
        int commentsSize = 5;

        doInJPA(entityManager -> {
            LongStream.range(0, 50).forEach(i -> {
                Post post = new Post(i);
                post.setTitle(String.format("Post nr. %d", i));

                LongStream.range(0, commentsSize).forEach(j -> {
                    PostComment comment = new PostComment();
                    comment.setId((i * commentsSize) + j);
                    comment.setReview(String.format("Good review nr. %d", comment.getId()));
                    post.addComment(comment);

                });
                entityManager.persist(post);
            });
        });

        int pageStart = 20;
        int pageSize = 10;

        doInJPA(entityManager -> {
            List<PostCommentSummary> summaries = entityManager.createNamedQuery(
                "PostCommentSummary")
            .setFirstResult(pageStart)
            .setMaxResults(pageSize)
            .getResultList();
            assertEquals(10, summaries.size());
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            List<PostCommentSummary> summaries = session.createSQLQuery(
                "SELECT p.id as id, p.title as title, c.review as review " +
                "FROM post_comment c " +
                "JOIN post p ON c.post_id = p.id " +
                "ORDER BY p.id")
            .setFirstResult(pageStart)
            .setMaxResults(pageSize)
            .setResultTransformer(new AliasToBeanResultTransformer(PostCommentSummary.class))
            .list();
            assertEquals(pageSize, summaries.size());
        });
    }

    @NamedNativeQuery(
        name = "PostCommentSummary",
        query =
            "SELECT p.id as id, p.title as title, c.review as review " +
            "FROM post_comment c " +
            "JOIN post p ON c.post_id = p.id " +
            "ORDER BY p.id",
        resultSetMapping = "PostCommentSummary"
    )
    @SqlResultSetMapping(
        name = "PostCommentSummary",
        classes = @ConstructorResult(
            targetClass = PostCommentSummary.class,
            columns = {
                @ColumnResult(name = "id"),
                @ColumnResult(name = "title"),
                @ColumnResult(name = "review"),
            }
        )
    )
    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
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

        @ManyToOne
        private Post post;

        private String review;

        public PostComment() {
        }

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
