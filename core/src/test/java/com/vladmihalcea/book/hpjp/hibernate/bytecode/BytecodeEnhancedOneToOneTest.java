package com.vladmihalcea.book.hpjp.hibernate.bytecode;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.book.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

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
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setDetails(
                        new PostDetails()
                            .setCreatedBy("Vlad Mihalcea")
                            .setCreatedOn(
                                Timestamp.valueOf(
                                    LocalDateTime.of(2016, 10, 12, 0, 0, 0))
                            )
                    )
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
        });
    }
}
