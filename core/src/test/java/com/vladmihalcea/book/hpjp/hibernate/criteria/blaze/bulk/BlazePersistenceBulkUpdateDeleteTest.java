package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.bulk;

import com.blazebit.persistence.*;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BlazePersistenceBulkUpdateDeleteTest extends AbstractTest {

    private CriteriaBuilderFactory cbf;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory entityManagerFactory = super.newEntityManagerFactory();
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        cbf = config.createCriteriaBuilderFactory(entityManagerFactory);
        return entityManagerFactory;
    }

    @Test
    public void testBulk() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setStatus(PostStatus.APPROVED)
            );

            entityManager.persist(
                new Post()
                    .setId(2L)
                    .setTitle("Spam title")
            );

            entityManager.persist(
                new Post()
                    .setId(3L)
                    .setMessage("Spam message")
            );

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setPost(entityManager.getReference(Post.class, 1L))
                    .setMessage("Spam comment")
            );
        });

        doInJPA(entityManager -> {
            assertEquals(2, flagSpam(entityManager, Post.class));
            assertEquals(1, flagSpam(entityManager, PostComment.class));
        });

        doInJPA(entityManager -> {
            assertEquals(2,
                entityManager.createQuery("""
                    update Post
                    set updatedOn = :timestamp
                    where status = :status
                    """)
                .setParameter("timestamp", Timestamp.valueOf(LocalDateTime.now().minusDays(7)))
                .setParameter("status", PostStatus.SPAM)
                .executeUpdate()
            );

            assertEquals(1,
                entityManager.createQuery("""
                    update PostComment
                    set updatedOn = :timestamp
                    where status = :status
                    """)
                .setParameter("timestamp", Timestamp.valueOf(LocalDateTime.now().minusDays(3)))
                .setParameter("status", PostStatus.SPAM)
                .executeUpdate()
            );
        });

        doInJPA(entityManager -> {
            assertEquals(2, deleteSpam(entityManager, Post.class));
            assertEquals(1, deleteSpam(entityManager, PostComment.class));
        });
    }

    public <T extends PostModerate> int flagSpam(
            EntityManager entityManager,
            Class<T> postModerateClass) {

        UpdateCriteriaBuilder<T> builder = cbf
            .update(entityManager, postModerateClass)
            .set(PostModerate_.STATUS, PostStatus.SPAM)
            .set(PostModerate_.UPDATED_ON, new Date());

        String spamToken = "%spam%";

        if(Post.class.isAssignableFrom(postModerateClass)) {
            builder
                .whereOr()
                    .where(lower(Post_.MESSAGE))
                        .like().value(spamToken).noEscape()
                    .where(lower(Post_.TITLE))
                        .like().value(spamToken).noEscape()
            .endOr();
        } else if(PostComment.class.isAssignableFrom(postModerateClass)) {
            builder
                .where(lower(PostComment_.MESSAGE))
                    .like().value(spamToken).noEscape();
        }

        return builder.executeUpdate();
    }

    public <T extends PostModerate> int deleteSpam(
            EntityManager entityManager,
            Class<T> postModerateClass) {

        return cbf
            .delete(entityManager, postModerateClass)
            .where(PostModerate_.STATUS).eq(PostStatus.SPAM)
            .where(PostModerate_.UPDATED_ON).le(
                Timestamp.valueOf(
                    LocalDateTime.now().minusDays(
                        (Post.class.isAssignableFrom(postModerateClass)) ? 7 : 3
                    )
                )
            )
            .executeUpdate();
    }

    private static String lower(String property) {
        return String.format("lower(%s)", property);
    }
}
