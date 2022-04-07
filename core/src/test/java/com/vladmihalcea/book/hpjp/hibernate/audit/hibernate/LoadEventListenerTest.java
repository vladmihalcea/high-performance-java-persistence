package com.vladmihalcea.book.hpjp.hibernate.audit.hibernate;

import com.vladmihalcea.book.hpjp.hibernate.audit.hibernate.listener.EventListenerIntegrator;
import com.vladmihalcea.book.hpjp.hibernate.audit.hibernate.model.*;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.transaction.JPATransactionFunction;
import org.hibernate.integrator.spi.Integrator;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class LoadEventListenerTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
            PostCommentDetails.class,
            LoadEventLogEntry.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected Integrator integrator() {
        return EventListenerIntegrator.INSTANCE;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");

            PostComment comment1 = new PostComment()
                .setId(1L)
                .setReview("Good")
                .setPost(post);

            PostCommentDetails details1 = new PostCommentDetails()
                .setComment(comment1)
                .setVotes(10);

            PostComment comment2 = new PostComment()
                .setId(2L)
                .setReview("Excellent")
                .setPost(post);

            PostCommentDetails details2 = new PostCommentDetails()
                .setComment(comment2)
                .setVotes(10);

            entityManager.persist(post);
            entityManager.persist(comment1);
            entityManager.persist(comment2);
            entityManager.persist(details1);
            entityManager.persist(details2);
        });
    }

    @Test
    public void testFindPost() {
        LoggedUser.logIn("vlad@vladmihalcea.com");

        assertTrue(getLoadEventLogEntries().isEmpty());

        Post post = doInJPA(entityManager -> {
            return entityManager.find(Post.class, 1L);
        });

        assertEquals("High-Performance Java Persistence", post.getTitle());

        List<LoadEventLogEntry> logEntries = getLoadEventLogEntries();
        assertEquals(1, logEntries.size());

        LoadEventLogEntry eventLogEntry = logEntries.get(0);
        assertPostEntityLogEntry(post, eventLogEntry);
    }

    @Test
    public void testFindPostComment() {
        LoggedUser.logIn("vlad@vladmihalcea.com");

        assertTrue(getLoadEventLogEntries().isEmpty());

        PostComment postComment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L);
        });

        assertEquals("Good", postComment.getReview());

        List<LoadEventLogEntry> logEntries = getLoadEventLogEntries();
        assertEquals(1, logEntries.size());

        LoadEventLogEntry eventLogEntry = logEntries.get(0);
        assertPostCommentEntityLogEntry(postComment, eventLogEntry);
    }

    @Test
    public void testQueryJoinFetch() {
        LoggedUser.logIn("vlad@vladmihalcea.com");

        PostCommentDetails postCommentDetails = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select pcd
                from PostCommentDetails pcd
                join fetch pcd.comment pc
                join fetch pc.post p
                where pcd.id = :id
                """, PostCommentDetails.class)
                .setParameter("id", 2L)
                .getSingleResult();
        });

        Map<String, LoadEventLogEntry> logEntryMap = getLoadEventLogEntryMap();
        assertEquals(3, logEntryMap.size());

        assertPostCommentDetailsEntityLogEntry(
            postCommentDetails,
            logEntryMap.get(PostCommentDetails.class.getName())
        );

        assertPostCommentEntityLogEntry(
            postCommentDetails.getComment(),
            logEntryMap.get(PostComment.class.getName())
        );

        assertPostEntityLogEntry(
            postCommentDetails.getComment().getPost(),
            logEntryMap.get(Post.class.getName())
        );
    }

    @Test
    public void testLazyLoading() {
        LoggedUser.logIn("vlad@vladmihalcea.com");

        doInJPA(entityManager -> {
            PostCommentDetails postCommentDetails = entityManager.find(PostCommentDetails.class, 2L);

            Map<String, LoadEventLogEntry> logEntryMap = getLoadEventLogEntryMap(
                getLoadEventLogEntries(entityManager)
            );
            assertEquals(1, logEntryMap.size());

            assertPostCommentDetailsEntityLogEntry(
                postCommentDetails,
                logEntryMap.get(PostCommentDetails.class.getName())
            );

            PostComment postComment = postCommentDetails.getComment();
            assertEquals("Excellent", postComment.getReview());

            logEntryMap = getLoadEventLogEntryMap(
                getLoadEventLogEntries(entityManager)
            );
            assertEquals(2, logEntryMap.size());

            assertPostCommentEntityLogEntry(
                postComment,
                logEntryMap.get(PostComment.class.getName())
            );

            Post post = postComment.getPost();
            assertEquals("High-Performance Java Persistence", post.getTitle());

            logEntryMap = getLoadEventLogEntryMap(
                getLoadEventLogEntries(entityManager)
            );
            assertEquals(3, logEntryMap.size());

            assertPostEntityLogEntry(
                post,
                logEntryMap.get(Post.class.getName())
            );
        });
    }

    public List<LoadEventLogEntry> getLoadEventLogEntries() {
        return doInJPA((JPATransactionFunction<List<LoadEventLogEntry>>) this::getLoadEventLogEntries);
    }

    public List<LoadEventLogEntry> getLoadEventLogEntries(EntityManager entityManager) {
        return entityManager.createQuery("""
            select le
            from LoadEventLogEntry le
            order by le.id desc
            """, LoadEventLogEntry.class)
        .getResultList();
    }

    public Map<String, LoadEventLogEntry> getLoadEventLogEntryMap() {
        return getLoadEventLogEntryMap(getLoadEventLogEntries());
    }

    public Map<String, LoadEventLogEntry> getLoadEventLogEntryMap(List<LoadEventLogEntry> logEntries) {
        return logEntries
            .stream()
            .collect(
                Collectors.toMap(
                    LoadEventLogEntry::getEntityName,
                    Function.identity()
                )
            );
    }

    private void assertPostEntityLogEntry(
            Post post, LoadEventLogEntry eventLogEntry) {
        assertEquals(
            "vlad@vladmihalcea.com",
            eventLogEntry.getCreatedBy()
        );

        assertEquals(
            Post.class.getName(),
            eventLogEntry.getEntityName()
        );

        assertEquals(
            String.valueOf(post.getId()),
            eventLogEntry.getEntityId()
        );
    }

    private void assertPostCommentEntityLogEntry(PostComment postComment, LoadEventLogEntry eventLogEntry) {
        assertEquals(
            "vlad@vladmihalcea.com",
            eventLogEntry.getCreatedBy()
        );

        assertEquals(
            PostComment.class.getName(),
            eventLogEntry.getEntityName()
        );

        assertEquals(
            String.valueOf(postComment.getId()),
            eventLogEntry.getEntityId()
        );
    }

    private void assertPostCommentDetailsEntityLogEntry(PostCommentDetails postCommentDetails, LoadEventLogEntry eventLogEntry) {
        assertEquals(
            "vlad@vladmihalcea.com",
            eventLogEntry.getCreatedBy()
        );

        assertEquals(
            PostCommentDetails.class.getName(),
            eventLogEntry.getEntityName()
        );

        assertEquals(
            String.valueOf(postCommentDetails.getId()),
            eventLogEntry.getEntityId()
        );
    }
}
