package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public class OptimisticLockingBidirectionalChildUpdatesRootVersionTest extends AbstractTest {

    public static class RootAwareEventListenerIntegrator implements Integrator {

        public static final RootAwareEventListenerIntegrator INSTANCE = new RootAwareEventListenerIntegrator();

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            final EventListenerRegistry eventListenerRegistry =
                    serviceRegistry.getService( EventListenerRegistry.class );

            eventListenerRegistry.appendListeners( EventType.PERSIST, RootAwareInsertEventListener.INSTANCE);
            eventListenerRegistry.appendListeners( EventType.FLUSH_ENTITY, RootAwareUpdateAndDeleteEventListener.INSTANCE);
        }

        @Override
        public void disintegrate(
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

        }
    }

    public static class RootAwareInsertEventListener implements PersistEventListener {

        private static final Logger LOGGER = LoggerFactory.getLogger(RootAwareInsertEventListener.class);

        public static final RootAwareInsertEventListener INSTANCE = new RootAwareInsertEventListener();

        @Override
        public void onPersist(PersistEvent event) throws HibernateException {
            final Object entity = event.getObject();

            if(entity instanceof RootAware) {
                RootAware rootAware = (RootAware) entity;
                Object root = rootAware.root();
                event.getSession().lock(root, LockMode.OPTIMISTIC_FORCE_INCREMENT);

                LOGGER.info("Incrementing {} entity version because a {} child entity has been inserted", root, entity);
            }
        }

        @Override
        public void onPersist(PersistEvent event, PersistContext persistContext) throws HibernateException {
            onPersist(event);
        }
    }

    public static class RootAwareUpdateAndDeleteEventListener implements FlushEntityEventListener {

        private static final Logger LOGGER = LoggerFactory.getLogger(RootAwareUpdateAndDeleteEventListener.class);

        public static final RootAwareUpdateAndDeleteEventListener INSTANCE = new RootAwareUpdateAndDeleteEventListener();

        @Override
        public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
            final EntityEntry entry = event.getEntityEntry();
            final Object entity = event.getEntity();
            final boolean mightBeDirty = entry.requiresDirtyCheck( entity );

            if(mightBeDirty && entity instanceof RootAware) {
                RootAware rootAware = (RootAware) entity;
                if(updated(event)) {
                    Object root = rootAware.root();
                    LOGGER.info("Incrementing {} entity version because a {} child entity has been updated", root, entity);
                    incrementRootVersion(event, root);
                }
                else if (deleted(event)) {
                    Object root = rootAware.root();
                    LOGGER.info("Incrementing {} entity version because a {} child entity has been deleted", root, entity);
                    incrementRootVersion(event, root);
                }
            }
        }

        private void incrementRootVersion(FlushEntityEvent event, Object root) {
            EntityEntry entityEntry = event.getSession().getPersistenceContext().getEntry( Hibernate.unproxy( root) );
            if(entityEntry.getStatus() != Status.DELETED) {
                event.getSession().lock(root, LockMode.OPTIMISTIC_FORCE_INCREMENT);
            }
        }

        private boolean deleted(FlushEntityEvent event) {
            return event.getEntityEntry().getStatus() == Status.DELETED;
        }

        private boolean updated(FlushEntityEvent event) {
            final EntityEntry entry = event.getEntityEntry();
            final Object entity = event.getEntity();

            int[] dirtyProperties;
            EntityPersister persister = entry.getPersister();
            final Object[] values = event.getPropertyValues();
            SessionImplementor session = event.getSession();

            if ( event.hasDatabaseSnapshot() ) {
                dirtyProperties = persister.findModified( event.getDatabaseSnapshot(), values, entity, session );
            }
            else {
                dirtyProperties = persister.findDirty( values, entry.getLoadedState(), entity, session );
            }

            return dirtyProperties != null;
        }
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
            PostCommentDetails.class,
        };
    }

    @Override
    protected Integrator integrator() {
        return RootAwareEventListenerIntegrator.INSTANCE;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Good");
            post.addComment( comment1 );

            PostCommentDetails details1 = new PostCommentDetails();
            details1.setComment(comment1);
            details1.setVotes(10);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent");
            post.addComment( comment2 );

            PostCommentDetails details2 = new PostCommentDetails();
            details2.setComment(comment2);
            details2.setVotes(10);

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class, 1L);

            entityManager.remove(post);
        });
    }

    public interface RootAware<T> {
        T root();
    }

    @Entity(name = "Post") @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private Integer version;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

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

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment implements RootAware<Post> {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        @OneToOne(mappedBy = "comment", cascade = CascadeType.ALL)
        private PostCommentDetails details;

        private String review;

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

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }

        @Override
        public Post root() {
            return post;
        }

        public void setDetails(PostCommentDetails details) {
            this.details = details;
        }
    }

    @Entity(name = "PostCommentDetails")
    @Table(name = "post_comment_details")
    public static class PostCommentDetails implements RootAware<Post> {

        @Id
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private PostComment comment;

        private int votes;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public PostComment getComment() {
            return comment;
        }

        public void setComment(PostComment comment) {
            this.comment = comment;
            comment.setDetails( this );
        }

        public int getVotes() {
            return votes;
        }

        public void setVotes(int votes) {
            this.votes = votes;
        }

        @Override
        public Post root() {
            return comment.root();
        }
    }
}
