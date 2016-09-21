package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.forum.*;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

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
}
