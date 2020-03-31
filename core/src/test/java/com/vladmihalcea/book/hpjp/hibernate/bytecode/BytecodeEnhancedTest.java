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
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .addComment(
                        new PostComment()
                            .setId(1L)
                            .setReview("Good")
                    )
                    .addComment(
                        new PostComment()
                            .setId(2L)
                            .setReview("Excellent")
                    )
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            post.setTitle("High-Performance Java Persistence, 2nd edition");
            entityManager.flush();
        });
    }
}
