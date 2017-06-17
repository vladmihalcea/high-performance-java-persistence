package com.vladmihalcea.book.hpjp.hibernate.association;

import java.util.List;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.book.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToOneLazyNoProxyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
                PostDetails.class,
                PostComment.class,
                Tag.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post1 = new Post("First post");
            post1.setId( 1L );

            PostDetails details1 = new PostDetails();
            details1.setCreatedBy( "John Doe" );
            post1.addDetails(details1);

            Post post2 = new Post("Second post");
            post2.setId( 2L );

            entityManager.persist(post1);
            entityManager.persist(post2);
        });
        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery(
                    "select p " +
                    "from Post p ", Post.class)
                .getResultList();
        });

        try {
            assertNotNull(posts.get( 0 ).getDetails());
            fail("Should throw LazyInitializationException");
        }
        catch (Exception expected) {
            LOGGER.info( "The @OneToOne association was fetched lazily" );
        }

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                    "select p " +
                    "from Post p " +
                    "where p.id = :postId", Post.class)
                    .setParameter( "postId", 2L )
                    .getSingleResult();

            LOGGER.info( "Fetched Post" );
            assertNull(post.getDetails());
        });
    }
}
