package com.vladmihalcea.book.hpjp.hibernate.bytecode;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.book.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public class BytecodeEnhancedOneToOneTest extends AbstractTest {

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
    public void testDirtyChecking() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");
            post.setId(1L);
            PostDetails details = new PostDetails();
            post.addDetails(details);
            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
        });
    }
}
