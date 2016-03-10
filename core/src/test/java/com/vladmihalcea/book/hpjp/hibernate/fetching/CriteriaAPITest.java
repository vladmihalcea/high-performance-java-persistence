package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.forum.*;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import static org.junit.Assert.assertNotNull;

/**
 * <code>FindEntityTest</code> - Find entity Test
 *
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
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Post> criteria = builder.createQuery(Post.class);
            Root<Post> fromPost = criteria.from(Post.class);
            criteria.where(builder.like(fromPost.get(Post_.title), "high-performance%"));
            Post post = entityManager.createQuery(criteria).getSingleResult();
            assertNotNull(post);
        });
    }
}
