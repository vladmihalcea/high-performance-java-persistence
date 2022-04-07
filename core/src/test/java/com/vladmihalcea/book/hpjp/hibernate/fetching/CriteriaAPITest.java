package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.forum.*;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.query.Query;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.transform.Transformers;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaAPITest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostComment.class,
            Tag.class,
            PostDetails.class
        };
    }


    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setDetails(
                    new PostDetails()
                        .setCreatedOn(new Date())
                        .setCreatedBy("Vlad Mihalcea")
                );

            entityManager.persist(post);

            for (long i = 0; i < 5; i++) {
                post.addComment(
                    new PostComment()
                        .setId(i + 1)
                        .setReview("Great")
                );
            }
        });
    }

    @Test
    public void testFind() {
        doInJPA(entityManager -> {
            List<Post> posts = filterPosts(entityManager, "High-Performance Java Persistence");
            assertFalse(posts.isEmpty());
        });
        doInJPA(entityManager -> {
            List<Post> posts = filterPosts(entityManager, null);
            assertTrue(posts.isEmpty());
        });
    }

    @Test
    public void testFilterChildWithoutMetamodel() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<PostComment> query = builder.createQuery(PostComment.class);
            Root<PostComment> postComment = query.from(PostComment.class);

            Join<PostComment, Post> post = postComment.join("post");

            query.where(
                builder.equal(
                    post.get("title"),
                    "High-Performance Java Persistence"
                )
            );

            List<PostComment> comments = entityManager
                .createQuery(query)
                .getResultList();

            assertEquals(5, comments.size());
        });
    }

    @Test
    public void testFilterChildWithMetamodel() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<PostComment> query = builder.createQuery(PostComment.class);
            Root<PostComment> postComment = query.from(PostComment.class);

            Join<PostComment, Post> post = postComment.join(PostComment_.post);

            query.where(
                builder.equal(
                    post.get(Post_.title),
                    "High-Performance Java Persistence"
                )
            );

            List<PostComment> comments = entityManager
                .createQuery(query)
                .getResultList();

            assertEquals(5, comments.size());
        });
    }

    private List<Post> filterPosts(EntityManager entityManager, String titlePattern) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Post> criteria = builder.createQuery(Post.class);
        Root<Post> post = criteria.from(Post.class);

        post.fetch(Post_.comments, JoinType.LEFT);

        Predicate titlePredicate = titlePattern == null ?
            builder.isNull(post.get(Post_.title)) :
            builder.like(post.get(Post_.title), titlePattern);

        criteria.where(titlePredicate);
        List<Post> posts = entityManager.createQuery(criteria).getResultList();

        return posts;
    }

    @Test
    public void testFetchObjectArray() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object[]> criteria = builder.createQuery(Object[].class);
            Root<PostComment> root = criteria.from(PostComment.class);
            criteria.multiselect(root.get(PostComment_.id), root.get(PostComment_.review));

            Join<PostComment, Post> postJoin = root.join("post");

            criteria.where(builder.like(postJoin.get(Post_.title), "High-Performance Java Persistence"));
            List<Object[]> comments = entityManager.createQuery(criteria).getResultList();

            assertEquals(5, comments.size());
        });
    }

    @Test
    public void testFetchObjectArrayToDTO() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);

            Root<PostComment> postComment = query.from(PostComment.class);
            Join<PostComment, Post> post = postComment.join(PostComment_.post);

            query.multiselect(
                postComment.get(PostComment_.id).alias(PostComment_.ID),
                postComment.get(PostComment_.review).alias(PostComment_.REVIEW),
                post.get(Post_.title).alias(Post_.TITLE)
            );

            query.where(
                builder.and(
                    builder.like(
                        post.get(Post_.title),
                        "%Java Persistence%"
                    ),
                    builder.equal(
                        post.get(Post_.details).get(PostDetails_.CREATED_BY),
                        "Vlad Mihalcea"
                    )
                )
            );

            List<PostCommentSummary> comments = entityManager
                .createQuery(query)
                .unwrap(Query.class)
                .setResultTransformer(Transformers.aliasToBean(PostCommentSummary.class))
                .getResultList();

            assertEquals(5, comments.size());
        });
    }
}
