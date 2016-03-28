package com.vladmihalcea.book.hpjp.hibernate.bytecode;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.book.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

/**
 * BytecodeEnhancedTest - Test to check dirty checking capabilities
 *
 * @author Vlad Mihalcea
 */
public class BytecodeEnhancedTest extends AbstractTest {

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
            Post post = new Post(1L);
            post.setTitle("Postit");

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Good");

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent");

            post.addComment(comment1);
            post.addComment(comment2);
            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            post.setTitle("Post it");
            entityManager.flush();
        });
    }
}
