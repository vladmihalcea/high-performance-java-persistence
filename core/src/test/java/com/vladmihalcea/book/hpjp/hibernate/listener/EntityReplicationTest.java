package com.vladmihalcea.book.hpjp.hibernate.listener;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.Test;

import java.time.LocalDate;

/**
 * @author Vlad Mihalcea
 */
public class EntityReplicationTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            OldPost.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post1 = new Post();
            post1.setId(1L);
            post1.setTitle("The High-Performance Java Persistence book is to be released!");

            entityManager.persist(post1);
        });

        doInJPA(entityManager -> {
            Post post1 = entityManager.find(Post.class, 1L);
            post1.setTitle(post1.getTitle().replace("to be ", ""));

            Post post2 = new Post();
            post2.setId(2L);
            post2.setTitle("The High-Performance Java Persistence book is awesome!");

            entityManager.persist(post2);
        });

        doInJPA(entityManager -> {
            entityManager.remove(entityManager.getReference(Post.class, 1L));
        });
    }

    @Override
    protected Integrator integrator() {
        return ReplicationEventListenerIntegrator.INSTANCE;
    }

    public static class ReplicationEventListenerIntegrator implements Integrator {

        public static final ReplicationEventListenerIntegrator INSTANCE = new ReplicationEventListenerIntegrator();

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            final EventListenerRegistry eventListenerRegistry =
                    serviceRegistry.getService(EventListenerRegistry.class);

            eventListenerRegistry.appendListeners(EventType.POST_INSERT, ReplicationInsertEventListener.INSTANCE);
            eventListenerRegistry.appendListeners(EventType.POST_UPDATE, ReplicationUpdateEventListener.INSTANCE);
            eventListenerRegistry.appendListeners(EventType.PRE_DELETE, ReplicationDeleteEventListener.INSTANCE);
        }

        @Override
        public void disintegrate(
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

        }
    }

    public static class ReplicationInsertEventListener implements PostInsertEventListener {

        public static final ReplicationInsertEventListener INSTANCE = new ReplicationInsertEventListener();

        @Override
        public void onPostInsert(PostInsertEvent event) throws HibernateException {
            final Object entity = event.getEntity();

            if(entity instanceof Post) {
                Post post = (Post) entity;

                event.getSession().createNativeQuery(
                    "INSERT INTO old_post (id, title, version) " +
                    "VALUES (:id, :title, :version)")
                .setParameter("id", post.getId())
                .setParameter("title", post.getTitle())
                .setParameter("version", post.getVersion())
                .setHibernateFlushMode(FlushMode.MANUAL)
                .executeUpdate();
            }
        }

        @Override
        public boolean requiresPostCommitHandling(EntityPersister persister) {
            return false;
        }
    }

    public static class ReplicationUpdateEventListener implements PostUpdateEventListener {

        public static final ReplicationUpdateEventListener INSTANCE = new ReplicationUpdateEventListener();

        @Override
        public void onPostUpdate(PostUpdateEvent event) {
            final Object entity = event.getEntity();

            if(entity instanceof Post) {
                Post post = (Post) entity;

                event.getSession().createNativeQuery(
                    "UPDATE old_post " +
                    "SET title = :title, version = :version " +
                    "WHERE id = :id")
                .setParameter("id", post.getId())
                .setParameter("title", post.getTitle())
                .setParameter("version", post.getVersion())
                .setHibernateFlushMode(FlushMode.MANUAL)
                .executeUpdate();
            }
        }

        @Override
        public boolean requiresPostCommitHandling(EntityPersister persister) {
            return false;
        }
    }

    public static class ReplicationDeleteEventListener implements PreDeleteEventListener {

        public static final ReplicationDeleteEventListener INSTANCE = new ReplicationDeleteEventListener();

        @Override
        public boolean onPreDelete(PreDeleteEvent event) {
            final Object entity = event.getEntity();

            if(entity instanceof Post) {
                Post post = (Post) entity;

                event.getSession().createNativeQuery(
                    "DELETE FROM old_post " +
                    "WHERE id = :id")
                .setParameter("id", post.getId())
                .setHibernateFlushMode(FlushMode.MANUAL)
                .executeUpdate();
            }

            return false;
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private LocalDate createdOn = LocalDate.now();

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

        public LocalDate getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(LocalDate createdOn) {
            this.createdOn = createdOn;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }

    @Entity(name = "OldPost")
    @Table(name = "old_post")
    public static class OldPost {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

        @MapsId
        @OneToOne
        @JoinColumn(name = "id")
        private Post post;

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

        public void setVersion(int version) {
            this.version = version;
        }
    }
}
