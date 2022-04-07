package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * CascadeLockTest - Test to check CascadeType.LOCK
 *
 * @author Vlad Mihalcea
 */
public class CascadeLockElementCollectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostDetails.class
        };
    }

    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("Hibernate Master Class");
            post.addDetails(new PostDetails());
            post.addComment(new PostComment("Good post!"));
            post.addComment(new PostComment("Nice post!"));

            entityManager.persist(post);
        });
    }

    @Test
    public void testCascadeLockOnManagedEntityWithScope() throws InterruptedException {
        LOGGER.info("Test lock cascade for managed entity");
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.unwrap(Session.class)
            .buildLockRequest(
                new LockOptions(LockMode.PESSIMISTIC_WRITE))
            .setScope(true)
            .lock(post);
        });
    }

    @Test
    public void testCascadeLockOnManagedEntityWithJPA() throws InterruptedException {
        LOGGER.info("Test lock cascade for managed entity");
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.lock(post, LockModeType.PESSIMISTIC_WRITE, Collections.singletonMap(
                AvailableSettings.JAKARTA_LOCK_SCOPE, PessimisticLockScope.EXTENDED
            ));
        });
    }

    @Test
    public void testCascadeLockOnManagedEntityWithQuery() throws InterruptedException {
        LOGGER.info("Test lock cascade for managed entity");
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.details " +
                "join fetch p.comments " +
                "where p.id = :id", Post.class)
            .setParameter("id", 1L)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();
        });
    }

    @Test
    public void testCascadeLockOnManagedEntityWithAssociationsInitialzied() throws InterruptedException {
        LOGGER.info("Test lock cascade for managed entity");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            Post post = (Post) entityManager.createQuery(
                    "select p " +
                            "from Post p " +
                            "join fetch p.details " +
                            "join fetch p.comments " +
                            "where " +
                            "   p.id = :id"
            ).setParameter("id", 1L)
                    .getSingleResult();
            session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).setScope(true).lock(post);
        });
    }

    @Test
    public void testCascadeLockOnManagedEntityWithAssociationsInitializedAndJpa() throws InterruptedException {
        LOGGER.info("Test lock cascade for managed entity");
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                    "select p " +
                            "from Post p " +
                            "join fetch p.details " +
                            "where p.id = :id", Post.class)
                    .setParameter("id", 1L)
                    .getSingleResult();
            entityManager.lock(post, LockModeType.PESSIMISTIC_WRITE, Collections.singletonMap(
                AvailableSettings.JAKARTA_LOCK_SCOPE, PessimisticLockScope.EXTENDED
            ));
        });
    }

    @Test
    public void testCascadeLockOnManagedEntityWithAssociationsUninitializedAndJpa() throws InterruptedException {
        LOGGER.info("Test lock cascade for managed entity");
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.lock(post, LockModeType.PESSIMISTIC_WRITE, Collections.singletonMap(
                AvailableSettings.JAKARTA_LOCK_SCOPE, PessimisticLockScope.EXTENDED
            ));
        });
    }

    private void containsPost(EntityManager entityManager, Post post, boolean expected) {
        assertEquals(expected, entityManager.contains(post));
        assertEquals(expected, (entityManager.contains(post.getDetails())));
    }

    @Test
    public void testCascadeLockOnDetachedEntityWithoutScope() {
        LOGGER.info("Test lock cascade for detached entity without scope");

        //Load the Post entity, which will become detached
        Post post = doInJPA(entityManager ->
            (Post) entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.details " +
                "join fetch p.comments " +
                "where p.id = :id"
        ).setParameter("id", 1L)
        .getSingleResult());

        //Change the detached entity state
        post.setTitle("Hibernate Training");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            //The Post entity graph is detached
            containsPost(entityManager, post, false);

            //The Lock request associates the entity graph and locks the requested entity
            session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).lock(post);

            //Hibernate doesn't know if the entity is dirty
            assertEquals("Hibernate Training", post.getTitle());

            //The Post entity graph is attached
            containsPost(entityManager, post, true);
        });
        doInJPA(entityManager -> {
            //The detached Post entity changes have been lost
            Post _post = (Post) entityManager.find(Post.class, 1L);
            assertEquals("Hibernate Master Class", _post.getTitle());
        });
    }

    @Test
    public void testCascadeLockOnDetachedEntityWithScope() {
        LOGGER.info("Test lock cascade for detached entity with scope");

        //Load the Post entity, which will become detached
        Post post = doInJPA(entityManager -> (Post) entityManager.createQuery(
            "select p " +
            "from Post p " +
            "join fetch p.details " +
            "join fetch p.comments " +
            "where p.id = :id", Post.class)
        .setParameter("id", 1L)
        .getSingleResult());

        doInJPA(entityManager -> {
            LOGGER.info("Reattach and lock");
            entityManager.unwrap(Session.class)
            .buildLockRequest(
                new LockOptions(LockMode.PESSIMISTIC_WRITE))
            .setScope(true)
            .lock(post);

            //The Post entity graph is attached
            containsPost(entityManager, post, true);
        });
        doInJPA(entityManager -> {
            //The detached Post entity changes have been lost
            Post _post = (Post) entityManager.find(Post.class, 1L);
            assertEquals("Hibernate Master Class", _post.getTitle());
        });
    }

    @Test
    public void testCascadeLockOnDetachedEntityUninitializedWithScope() {
        LOGGER.info("Test lock cascade for detached entity with scope");

        //Load the Post entity, which will become detached
        Post post = doInJPA(entityManager -> (Post) entityManager.find(Post.class, 1L));

        doInJPA(entityManager -> {
            LOGGER.info("Reattach and lock entity with associations not initialized");
            entityManager.unwrap(Session.class)
                    .buildLockRequest(
                            new LockOptions(LockMode.PESSIMISTIC_WRITE))
                    .setScope(true)
                    .lock(post);

            LOGGER.info("Check entities are reattached");
            //The Post entity graph is attached
            containsPost(entityManager, post, true);
        });
    }

    @Test
    public void testUpdateOnDetachedEntity() {
        LOGGER.info("Test update for detached entity");
        //Load the Post entity, which will become detached
        Post post = doInJPA(entityManager -> (Post) entityManager.createQuery(
            "select p " +
            "from Post p " +
            "join fetch p.details " +
            "join fetch p.comments " +
            "where p.id = :id", Post.class)
        .setParameter("id", 1L)
        .getSingleResult());

        //Change the detached entity state
        post.setTitle("Hibernate Training");

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            //The Post entity graph is detached
            containsPost(entityManager, post, false);

            //The update will trigger an entity state flush and attach the entity graph
            session.update(post);

            //The Post entity graph is attached
            containsPost(entityManager, post, true);
        });
        doInJPA(entityManager -> {
            Post _post = (Post) entityManager.find(Post.class, 1L);
            assertEquals("Hibernate Training", _post.getTitle());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        private String body;

        @Version
        private int version;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @ElementCollection
        private List<PostComment> comments = new ArrayList<>();

        @OneToOne(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true, fetch = FetchType.LAZY, optional = false)
        private PostDetails details;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<PostComment> getComments() {
            return comments;
        }

        public PostDetails getDetails() {
            return details;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
        }

        public void addDetails(PostDetails details) {
            this.details = details;
            details.setPost(this);
        }

        public void removeDetails() {
            this.details.setPost(null);
            this.details = null;
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @Version
        private int version;

        public PostDetails() {
            createdOn = new Date();
        }

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }

    @Embeddable
    public static class PostComment {

        private String review;

        public PostComment() {}

        public PostComment(String review) {
            this.review = review;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
