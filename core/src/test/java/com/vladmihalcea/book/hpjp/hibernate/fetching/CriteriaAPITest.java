package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.forum.*;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.query.Query;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.transform.Transformers;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
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
            Post post = new Post();
            post.setId(1L);
            post.setTitle("high-performance-java-persistence");
            entityManager.persist(post);

            for (long i = 0; i < 5; i++) {
                PostComment comment = new PostComment();
                comment.setId(i + 1);
                comment.setReview("Great");
                post.addComment(comment);
            }
        });
    }

    @Test
    public void testFind() {
        doInJPA(entityManager -> {
            List<Post> posts = filterPosts(entityManager, "high-performance%");
            assertFalse(posts.isEmpty());
        });
        doInJPA(entityManager -> {
            List<Post> posts = filterPosts(entityManager, null);
            assertTrue(posts.isEmpty());
        });
    }

    @Test
    public void testFilterChild() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<PostComment> criteria = builder.createQuery(PostComment.class);
            Root<PostComment> fromPostComment = criteria.from(PostComment.class);

            Join<PostComment, Post> postJoin = fromPostComment.join("post");

            criteria.where(builder.like(postJoin.get(Post_.title), "high-performance%"));
            List<PostComment> comments = entityManager.createQuery(criteria).getResultList();

            assertEquals(5, comments.size());
        });
    }

    private List<Post> filterPosts(EntityManager entityManager, String titlePattern) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Post> criteria = builder.createQuery(Post.class);
        Root<Post> fromPost = criteria.from(Post.class);

        Predicate titlePredicate = titlePattern == null ?
            builder.isNull(fromPost.get(Post_.title)) :
            builder.like(fromPost.get(Post_.title), titlePattern);

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

            criteria.where(builder.like(postJoin.get(Post_.title), "high-performance%"));
            List<Object[]> comments = entityManager.createQuery(criteria).getResultList();

            assertEquals(5, comments.size());
        });
    }

    @Test
    public void testFetchObjectArrayToDTO() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object[]> criteria = builder.createQuery(Object[].class);

            Root<PostComment> root = criteria.from(PostComment.class);
            Join<PostComment, Post> postJoin = root.join("post");

            criteria.multiselect(
                root.get(PostComment_.id).alias("id"),
                root.get(PostComment_.review).alias("review"),
                postJoin.get(Post_.title).alias("title")
            );

            criteria.where(builder.like(postJoin.get(Post_.title), "high-performance%"));

            List<PostCommentSummary> comments = entityManager
                .createQuery(criteria)
                .unwrap(Query.class)
                .setResultTransformer(Transformers.aliasToBean(PostCommentSummary.class))
                .getResultList();

            assertEquals(5, comments.size());
        });
    }
}
