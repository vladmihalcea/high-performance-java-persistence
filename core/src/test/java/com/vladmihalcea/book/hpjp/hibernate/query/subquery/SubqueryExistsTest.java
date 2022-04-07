package com.vladmihalcea.book.hpjp.hibernate.query.subquery;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab.TabInstance;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class SubqueryExistsTest extends AbstractTest {

    private CriteriaBuilderFactory cbf;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory entityManagerFactory = super.newEntityManagerFactory();
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        cbf = config.createCriteriaBuilderFactory(entityManagerFactory);
        return entityManagerFactory;
    }
    
    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            
            String[] reviews = new String[] {
                "Amazing",
                "Great",
                "Excellent",
                "Highly recommended",
                "Simply the best"
            };
            
            long postId = 0;

            Post post1 = new Post()
                .setId(++postId)
                .setTitle("High-Performance Java Persistence");

            Post post2 = new Post()
                .setId(++postId)
                .setTitle("High-Performance SQL");

            entityManager.persist(post1);
            entityManager.persist(post2);
            
            long postCommentId = 0;

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post1)
                    .setReview(reviews[random.nextInt(reviews.length)])
                    .setScore(1)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post1)
                    .setReview(reviews[random.nextInt(reviews.length)])
                    .setScore(2)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post1)
                    .setReview(reviews[random.nextInt(reviews.length)])
                    .setScore(3)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post1)
                    .setReview(reviews[random.nextInt(reviews.length)])
                    .setScore(4)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post1)
                    .setReview("Highly recommended")
                    .setScore(5)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post1)
                    .setReview("Highly recommended")
                    .setScore(6)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post2)
                    .setReview(reviews[random.nextInt(reviews.length)])
                    .setScore(7)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post2)
                    .setReview("Simply the best")
                    .setScore(8)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post2)
                    .setReview("Simply the best")
                    .setScore(9)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post2)
                    .setReview("Highly recommended")
                    .setScore(10)
            );

            entityManager.persist(
                new PostComment()
                    .setId(++postCommentId)
                    .setPost(post2)
                    .setReview("Highly recommended")
                    .setScore(11)
            );
        });
    }

    @Test
    public void testJoin() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select distinct p
                from PostComment pc
                join pc.post p
                where pc.score > :minScore
                order by p.id
                """, Post.class)
            .setParameter("minScore", 10)
            .getResultList();

            assertSame(1, posts.size());

            Post post = posts.get(0);
            assertEquals(2L, post.getId().longValue());
        });
    }

    @Test
    public void testExistsJPQL() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where exists (
                   select 1
                   from PostComment pc
                   where
                      pc.post = p and
                      pc.score > :minScore
                )
                order by p.id
                """, Post.class)
            .setParameter("minScore", 10)
            .getResultList();

            assertSame(1, posts.size());

            Post post = posts.get(0);
            assertEquals(2L, post.getId().longValue());
        });
    }
    
    @Test
    public void testExistsCriteriaAPI() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Post> query = builder.createQuery(Post.class);
            Root<Post> p = query.from(Post.class);

            ParameterExpression<Integer> minScore = builder.parameter(Integer.class);
            Subquery<Integer> subQuery = query.subquery(Integer.class);
            Root<PostComment> pc = subQuery.from(PostComment.class);
            subQuery
                .select(builder.literal(1))
                .where(
                    builder.equal(pc.get(PostComment_.POST), p),
                    builder.gt(pc.get(PostComment_.SCORE), minScore)
                );

            query.where(builder.exists(subQuery));

            List<Post> posts = entityManager.createQuery(query)
                .setParameter(minScore, 10)
                .getResultList();

            assertSame(1, posts.size());

            Post post = posts.get(0);
            assertEquals(2L, post.getId().longValue());
        });
    }

    @Test
    public void testExistsBlazePersistence() {
        doInJPA(entityManager -> {

            final String POST_ALIAS = "p";
            final String POST_COMMENT_ALIAS = "pc";

            List<Post> posts = cbf.create(entityManager, Post.class)
                .from(Post.class, POST_ALIAS)
                .whereExists()
                    .from(PostComment.class, POST_COMMENT_ALIAS)
                    .select("1")
                    .where(PostComment_.POST).eqExpression(POST_ALIAS)
                    .where(PostComment_.SCORE).gtExpression(":minScore")
                .end()
                .select(POST_ALIAS)
                .setParameter("minScore", 10)
                .getResultList();

            assertSame(1, posts.size());

            Post post = posts.get(0);
            assertEquals(2L, post.getId().longValue());
        });
    }

}
