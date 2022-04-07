package com.vladmihalcea.book.hpjp.hibernate.mapping;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public class HydratedStateListenerTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put( AvailableSettings.STATEMENT_BATCH_SIZE, 10 );
        return properties;
    }

    @Test
    public void test() {

        doInJPA(entityManager -> {
            Post post1 = new Post();
            post1.setId(1L);
            post1.setTitle("High-Performance Java Persistence");
            entityManager.persist(post1);

            Post post2 = new Post();
            post2.setId(2L);
            post2.setTitle("High-Performance Java Persistence");
            entityManager.persist(post2);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            LOGGER.info("Fetched post: {}", post);
            post.setScore(12);

            SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) entityManager.unwrap( Session.class ).getSessionFactory();

            sessionFactory.getServiceRegistry()
                    .getService( EventListenerRegistry.class )
                    .prependListeners( EventType.FLUSH_ENTITY, new HydratedStateFlushEntityEventListener() );

            entityManager.flush();

            Post post2 = entityManager.find(Post.class, 2L);
            LOGGER.info("Fetched post: {}", post2);
            post2.setTitle("HPJP");
        });
    }

    public static class HydratedStateFlushEntityEventListener implements FlushEntityEventListener {

        @Override
        public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
            String[] properties = event.getEntityEntry().getPersister().getPropertyNames();
            Object[] loadedState = event.getEntityEntry().getLoadedState();
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @DynamicUpdate
    public static class Post {

        @Id
        private Long id;

        private String title;

        private long score;

        @Column(name = "created_on", nullable = false, updatable = false)
        private Timestamp createdOn;

        @Transient
        private String creationTimestamp;

        public Post() {
            this.createdOn = new Timestamp(System.currentTimeMillis());
        }

        public String getCreationTimestamp() {
            if(creationTimestamp == null) {
                creationTimestamp = DateTimeFormatter.ISO_DATE_TIME.format(
                    createdOn.toLocalDateTime()
                );
            }
            return creationTimestamp;
        }

        @Override
        public String toString() {
            return String.format(
                "Post{\n" +
                "  id=%d\n" +
                "  title='%s'\n" +
                "  score=%d\n" +
                "  creationTimestamp='%s'\n" +
                '}', id, title, score, getCreationTimestamp()
            );
        }

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

        public long getScore() {
            return score;
        }

        public void setScore(long score) {
            this.score = score;
        }

        public Timestamp getCreatedOn() {
            return createdOn;
        }
    }
}
