package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * CascadeLockTest - Test to check CascadeType.LOCK
 *
 * @author Carol Mihalcea
 */
public class CascadeLockTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostDetails.class,
                Comment.class
        };
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setName("Hibernate Master Class");

            post.addDetails(new PostDetails());
            post.addComment(new Comment("Good post!"));
            post.addComment(new Comment("Nice post!"));

            entityManager.persist(post);
        });
    }

    @Test
    public void testCascadeLockOnManagedEntity() throws InterruptedException {
        LOGGER.info("Test lock cascade for managed entity");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            Post post = (Post) entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.details " +
                "where " +
                "   p.id = :id"
            ).setParameter("id", 1L)
            .getSingleResult();
            session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).setScope(true).lock(post);
        });
    }

    private void containsPost(EntityManager entityManager, Post post, boolean expected) {
        assertEquals(expected, entityManager.contains(post));
        assertEquals(expected, (entityManager.contains(post.getDetails())));
        for(Comment comment : post.getComments()) {
            assertEquals(expected, (entityManager.contains(comment)));
        }
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
                "where " +
                "   p.id = :id"
        ).setParameter("id", 1L)
        .getSingleResult());

        //Change the detached entity state
        post.setName("Hibernate Training");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            //The Post entity graph is detached
            containsPost(entityManager, post, false);

            //The Lock request associates the entity graph and locks the requested entity
            session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).lock(post);

            //Hibernate doesn't know if the entity is dirty
            assertEquals("Hibernate Training", post.getName());

            //The Post entity graph is attached
            containsPost(entityManager, post, true);
        });
        doInJPA(entityManager -> {
            //The detached Post entity changes have been lost
            Post _post = (Post) entityManager.find(Post.class, 1L);
            assertEquals("Hibernate Master Class", _post.getName());
        });
    }

    @Test
    public void testCascadeLockOnDetachedEntityWithScope() {
        LOGGER.info("Test lock cascade for detached entity with scope");

        //Load the Post entity, which will become detached
        Post post = doInJPA(entityManager ->
            (Post) entityManager.createQuery(
                "select p " +
                        "from Post p " +
                        "join fetch p.details " +
                        "join fetch p.comments " +
                        "where " +
                        "   p.id = :id"
        ).setParameter("id", 1L)
        .getSingleResult());

        //Change the detached entity state
        post.setName("Hibernate Training");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            //The Post entity graph is detached
            containsPost(entityManager, post, false);

            //The Lock request associates the entity graph and locks the requested entity
            session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).setScope(true).lock(post);

            //Hibernate doesn't know if the entity is dirty
            assertEquals("Hibernate Training", post.getName());

            //The Post entity graph is attached
            containsPost(entityManager, post, true);
        });
        doInJPA(entityManager -> {
            //The detached Post entity changes have been lost
            Post _post = (Post) entityManager.find(Post.class, 1L);
            assertEquals("Hibernate Master Class", _post.getName());
        });
    }

    @Test
    public void testUpdateOnDetachedEntity() {
        LOGGER.info("Test update for detached entity");
        //Load the Post entity, which will become detached
        Post post = doInJPA(entityManager ->
            (Post) entityManager.createQuery(
                "select p " +
                        "from Post p " +
                        "join fetch p.details " +
                        "join fetch p.comments " +
                        "where " +
                        "   p.id = :id"
        ).setParameter("id", 1L)
        .getSingleResult());

        //Change the detached entity state
        post.setName("Hibernate Training");

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
            assertEquals("Hibernate Training", _post.getName());
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;

        private String name;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
        private List<Comment> comments = new ArrayList<>();

        @OneToOne(cascade = CascadeType.ALL, mappedBy = "post", fetch = FetchType.LAZY)
        private PostDetails details;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Comment> getComments() {
            return comments;
        }

        public PostDetails getDetails() {
            return details;
        }

        public void addComment(Comment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void addDetails(PostDetails details) {
            this.details = details;
            details.setPost(this);
        }
    }

    @Entity(name = "PostDetails")
    public static class PostDetails {

        @Id
        private Long id;

        private Date createdOn;

        public PostDetails() {
            createdOn = new Date();
        }

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "id")
        @MapsId
        private Post post;

        public Long getId() {
            return id;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }

    @Entity(name = "Comment")
    public static class Comment {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;

        @ManyToOne
        private Post post;

        public Comment() {}

        public Comment(String review) {
            this.review = review;
        }

        private String review;

        public Long getId() {
            return id;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
