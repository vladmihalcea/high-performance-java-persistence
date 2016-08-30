package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.Test;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class OptimisticLockingChildUpdatesRootVersionTest extends AbstractTest {

    public static class RootAwareFlushEntityEventListenerIntegrator implements org.hibernate.integrator.spi.Integrator {

        public static final RootAwareFlushEntityEventListenerIntegrator INSTANCE = new RootAwareFlushEntityEventListenerIntegrator();

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            final EventListenerRegistry eventListenerRegistry =
                    serviceRegistry.getService( EventListenerRegistry.class );

            eventListenerRegistry.appendListeners(EventType.FLUSH_ENTITY, RootAwareFlushEntityEventListener.INSTANCE);
        }

        @Override
        public void disintegrate(
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

        }
    }

    public static class RootAwareFlushEntityEventListener implements FlushEntityEventListener {

        public static final RootAwareFlushEntityEventListener INSTANCE = new RootAwareFlushEntityEventListener();

        @Override
        public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
            final EntityEntry entry = event.getEntityEntry();
            final Object entity = event.getEntity();
            final boolean mightBeDirty = entry.requiresDirtyCheck( entity );

            if(mightBeDirty && entity instanceof RootAware) {
                RootAware rootAware = (RootAware) entity;
                event.getSession().lock(rootAware.root(), LockMode.OPTIMISTIC_FORCE_INCREMENT);
            }
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
        return RootAwareFlushEntityEventListenerIntegrator.INSTANCE;
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
            comment1.setPost(post);

            PostCommentDetails details1 = new PostCommentDetails();
            details1.setComment(comment1);
            details1.setVotes(10);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent");
            comment2.setPost(post);

            PostCommentDetails details2 = new PostCommentDetails();
            details2.setComment(comment2);
            details2.setVotes(10);

            entityManager.persist(post);
            entityManager.persist(comment1);
            entityManager.persist(comment2);
            entityManager.persist(details1);
            entityManager.persist(details2);
        });

        doInJPA(entityManager -> {
            PostCommentDetails postCommentDetails = entityManager.createQuery(
                "select pcd " +
                "from PostCommentDetails pcd " +
                "join fetch pcd.comment pc " +
                "join fetch pc.post p " +
                "where pcd.id = :id", PostCommentDetails.class)
            .setParameter("id", 2L)
            .getSingleResult();

            postCommentDetails.setVotes(15);
        });

        doInJPA(entityManager -> {
            PostComment postComment = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "join fetch pc.post p " +
                "where pc.id = :id", PostComment.class)
            .setParameter("id", 2L)
            .getSingleResult();

            postComment.setReview("Brilliant!");
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
        private int version;

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

        public int getVersion() {
            return version;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment implements RootAware<Post> {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

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
    }

    @Entity(name = "PostCommentDetails")
    @Table(name = "post_comment_details")
    public static class PostCommentDetails implements RootAware<Post> {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
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
        }

        public int getVotes() {
            return votes;
        }

        public void setVotes(int votes) {
            this.votes = votes;
        }

        @Override
        public Post root() {
            return comment.getPost();
        }
    }
}
