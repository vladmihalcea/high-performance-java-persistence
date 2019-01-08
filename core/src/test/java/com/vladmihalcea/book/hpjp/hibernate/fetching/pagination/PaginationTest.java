package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination;

import com.vladmihalcea.book.hpjp.hibernate.fetching.PostCommentSummary;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.query.NativeQuery;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PaginationTest extends AbstractTest {

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
    public void afterInit() {
        doInJPA(entityManager -> {
            LocalDateTime timestamp = LocalDateTime.of(
                2018, 10, 9, 12, 0, 0, 0
            );

            int commentsSize = 5;

            LongStream.range(1, 50).forEach(postId -> {
                Post post = new Post();
                post.setId(postId);
                post.setTitle(String.format("Post nr. %d", postId));
                post.setCreatedOn(
                     Timestamp.valueOf(timestamp.plusMinutes(postId))
                );

                LongStream.range(1, commentsSize + 1).forEach(commentOffset -> {
                    PostComment comment = new PostComment();

                    long commentId = ((postId - 1) * commentsSize) + commentOffset;
                    comment.setId(commentId);
                    comment.setReview(
                        String.format("Comment nr. %d", comment.getId())
                    );
                    comment.setCreatedOn(
                        Timestamp.valueOf(timestamp.plusMinutes(commentId))
                    );

                    post.addComment(comment);

                });
                entityManager.persist(post);
            });

            long postWithoutCommentsId = 51L;
            Post postWithoutComments = new Post();
            postWithoutComments.setId(postWithoutCommentsId);
            postWithoutComments.setTitle(String.format("Post nr. %d", postWithoutCommentsId));
            postWithoutComments.setCreatedOn(
                    Timestamp.valueOf(timestamp.plusMinutes(postWithoutCommentsId))
            );

            entityManager.persist(postWithoutComments);
        });
    }

    @Test
    public void testLimit() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "order by p.createdOn ")
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, posts.size());
            assertEquals("Post nr. 1", posts.get(0).getTitle());
            assertEquals("Post nr. 10", posts.get(9).getTitle());
        });
    }

    @Test
    public void testOffset() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "order by p.createdOn ")
            .setFirstResult(10)
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, posts.size());
            assertEquals("Post nr. 11", posts.get(0).getTitle());
            assertEquals("Post nr. 20", posts.get(9).getTitle());
        });
    }

    @Test
    public void testOffsetNative() {
        doInJPA(entityManager -> {
            List<Tuple> posts = entityManager.createNativeQuery(
                "select p.id as id, p.title as title " +
                "from post p " +
                "order by p.created_on", Tuple.class)
            .setFirstResult(10)
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, posts.size());
            assertEquals("Post nr. 11", posts.get(0).get("title"));
            assertEquals("Post nr. 20", posts.get(9).get("title"));
        });
    }

    @Test
    public void testDTO() {
        doInJPA(entityManager -> {
            List<PostCommentSummary> summaries = entityManager.createQuery(
                "select new " +
                "   com.vladmihalcea.book.hpjp.hibernate.fetching.PostCommentSummary( " +
                "       p.id, p.title, c.review " +
                "   ) " +
                "from PostComment c " +
                "join c.post p " +
                "order by c.createdOn")
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, summaries.size());
            assertEquals("Post nr. 1", summaries.get(0).getTitle());
            assertEquals("Comment nr. 1", summaries.get(0).getReview());

            assertEquals("Post nr. 2", summaries.get(9).getTitle());
            assertEquals("Comment nr. 10", summaries.get(9).getReview());
        });
    }

    @Test
    public void testFetchAndPaginate() {
        doInJPA(entityManager -> {

            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "left join fetch p.comments " +
                "order by p.createdOn", Post.class)
            .setMaxResults(10)
            .getResultList();

            assertEquals(10, posts.size());
        });
    }

    @Test
    public void testFetchAndPaginateUsingDenseRank() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createNamedQuery("PostWithCommentByRank")
            .setParameter("rank", 10)
            .unwrap(NativeQuery.class)
            .setResultTransformer(DistinctPostResultTransformer.INSTANCE)
            .getResultList();

            assertEquals(10, posts.size());


            /*
             * Checking the comments collections of the returned posts.
             * I'm just checking the post comments of the Post with the Id = 1
             *
             * The Expected comments list should contains something like
             *
             * id	    created_on	                    review	            post_id
             * 1	    2018-10-09 12:01:00.000000	    Comment nr. 1	    1
             * 2	    2018-10-09 12:02:00.000000	    Comment nr. 2	    1
             * 3	    2018-10-09 12:03:00.000000	    Comment nr. 3	    1
             * 4	    2018-10-09 12:04:00.000000	    Comment nr. 4	    1
             * 5	    2018-10-09 12:05:00.000000	    Comment nr. 5	    1
             */

            final Post post1 = posts.stream().filter(p -> p.getId() == 1L).findFirst().orElse(null);
            Assert.assertNotNull(post1);
            Assert.assertEquals(1L, post1.getId().longValue());
            final List<PostComment> comments = post1.getComments();
            assertEquals(5, comments.size());

            final PostComment postComment1 = comments.stream().filter(comment -> comment.getId() == 1L).findFirst().orElse(null);
            Assert.assertNotNull(postComment1);
            Assert.assertEquals("Comment nr. 1", postComment1.getReview());

            final PostComment postComment2 = comments.stream().filter(comment -> comment.getId() == 2L).findFirst().orElse(null);
            Assert.assertNotNull(postComment2);
            Assert.assertEquals("Comment nr. 2", postComment2.getReview());

            final PostComment postComment3 = comments.stream().filter(comment -> comment.getId() == 3L).findFirst().orElse(null);
            Assert.assertNotNull(postComment3);
            Assert.assertEquals("Comment nr. 3", postComment3.getReview());

            final PostComment postComment4 = comments.stream().filter(comment -> comment.getId() == 4L).findFirst().orElse(null);
            Assert.assertNotNull(postComment4);
            Assert.assertEquals("Comment nr. 4", postComment4.getReview());

            final PostComment postComment5 = comments.stream().filter(comment -> comment.getId() == 5L).findFirst().orElse(null);
            Assert.assertNotNull(postComment5);
            Assert.assertEquals("Comment nr. 5", postComment5.getReview());

        });
    }

    @Test
    public void testFetchAndPaginateUsingDenseRankPostWithoutComments() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createNamedQuery("PostWithCommentByRank")
                    .setParameter("rank", 51)
                    .unwrap(NativeQuery.class)
                    .setResultTransformer(DistinctPostResultTransformer.INSTANCE)
                    .getResultList();

            assertEquals(50, posts.size());
            final Post postWithoutComments = posts.stream().filter(p -> p.getId() == 51L).findFirst().orElse(null);
            Assert.assertNotNull(postWithoutComments);
            Assert.assertTrue(postWithoutComments.getComments().isEmpty());

            PostComment comment = new PostComment();
            long commentId = 247;
            comment.setId(commentId);
            comment.setReview(String.format("Comment nr. %d", comment.getId()));
            LocalDateTime timestamp = LocalDateTime.of(2018, 11, 9, 12, 0, 0, 0);
            comment.setCreatedOn(Timestamp.valueOf(timestamp.plusMinutes(commentId)));

            postWithoutComments.addComment(comment);
        });

        doInJPA(entityManager -> {
            final Post post = entityManager.find(Post.class, 51L);
            assertEquals(1, post.getComments().size());
        });
    }

}
