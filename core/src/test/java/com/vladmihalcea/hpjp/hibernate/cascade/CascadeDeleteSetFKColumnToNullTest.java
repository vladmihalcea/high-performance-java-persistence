package com.vladmihalcea.hpjp.hibernate.cascade;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class CascadeDeleteSetFKColumnToNullTest extends AbstractTest {

    public static class CustomEventListenerIntegrator implements Integrator {

        public static final CustomEventListenerIntegrator INSTANCE = new CustomEventListenerIntegrator();

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            final EventListenerRegistry eventListenerRegistry =
                    serviceRegistry.getService( EventListenerRegistry.class );

            eventListenerRegistry.appendListeners(EventType.DELETE, PostDeleteEventListener.INSTANCE);
        }

        @Override
        public void disintegrate(
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

        }
    }

    public static class PostDeleteEventListener implements DeleteEventListener {

        public static final PostDeleteEventListener INSTANCE = new PostDeleteEventListener();

        @Override
        public void onDelete(DeleteEvent event) throws HibernateException {
            final Object entity = event.getObject();

            if(entity instanceof Post post) {
                for(Iterator<PostComment> postCommentIterator = post.getComments().iterator(); postCommentIterator.hasNext();) {
                    PostComment comment = postCommentIterator.next();
                    comment.setPost(null);
                    postCommentIterator.remove();
                }
            }
        }

        @Override
        public void onDelete(DeleteEvent event, DeleteContext transientEntities) throws HibernateException {
            onDelete(event);
        }
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Integrator integrator() {
        return CustomEventListenerIntegrator.INSTANCE;
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

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent");
            comment2.setPost(post);

            post.addComment(comment1);
            post.addComment(comment2);

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                """, Post.class)
            .getSingleResult();

            for(PostComment comment : post.getComments()) {
                assertNotNull(comment.getPost());
            }

            entityManager.remove(post);
            entityManager.flush();

            for(PostComment comment : post.getComments()) {
                assertNull(comment.getPost());
            }
        });

        doInJPA(entityManager -> {
            List<PostComment> comments = entityManager.createQuery("""
                select pc
                from PostComment pc
                """, PostComment.class)
            .getResultList();

            for(PostComment comment : comments) {
                assertNull(comment.getPost());
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(
            mappedBy = "post",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
        )
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

        public List<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = true)
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
    }
}
