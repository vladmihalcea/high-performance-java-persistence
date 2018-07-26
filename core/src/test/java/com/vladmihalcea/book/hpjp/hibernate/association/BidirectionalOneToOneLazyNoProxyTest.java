package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.book.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToOneLazyNoProxyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostDetails.class,
            PostComment.class,
            Tag.class
        };
    }

    @Test
    @Ignore
    public void testNPlusOne() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence, 1st Part");

            PostDetails details = new PostDetails();
            details.setCreatedBy("Vlad Mihalcea");

            post.addDetails(details);

            entityManager.persist(post);
        });

        Post post = doInJPA(entityManager -> {
            return entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.title like 'High-Performance Java Persistence%'", Post.class)
            .getSingleResult();
        });

        try {
            assertNotNull(post.getDetails());

            fail("Should throw LazyInitializationException");
        } catch (Exception expected) {
            LOGGER.info("The @OneToOne association was fetched lazily");
        }
    }

    @Test
    public void testLazyLoading() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence, 2nd Part");

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.id = :postId", Post.class)
            .setParameter("postId", 1L)
            .getSingleResult();

            LOGGER.info("Fetched Post entity");

            assertNull(post.getDetails());
        });
    }
}
