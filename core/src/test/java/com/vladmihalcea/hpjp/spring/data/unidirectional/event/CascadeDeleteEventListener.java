package com.vladmihalcea.hpjp.spring.data.unidirectional.event;

import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.Post;
import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.PostDetails;
import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.PostTag;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class CascadeDeleteEventListener implements DeleteEventListener {

    public static final CascadeDeleteEventListener INSTANCE = new CascadeDeleteEventListener();

    @Override
    public void onDelete(DeleteEvent event) throws HibernateException {
        final Object entity = event.getObject();

        if (entity instanceof Post post) {
            Session session = event.getSession();

            PostDetails details = session.find(PostDetails.class, post.getId());
            session.remove(details);

            List<PostComment> comments = session.createQuery("""
                select pc
                from PostComment pc
                where pc.post.id = :postId
                """, PostComment.class)
            .setParameter("postId", post.getId())
            .getResultList();

            for(PostComment comment : comments) {
                session.remove(comment);
            }

            List<PostTag> postTags = session.createQuery("""
                select pt
                from PostTag pt
                where pt.post.id = :postId
                """, PostTag.class)
            .setParameter("postId", post.getId())
            .getResultList();

            for(PostTag postTag : postTags) {
                session.remove(postTag);
            }
        }
    }

    @Override
    public void onDelete(DeleteEvent event, DeleteContext transientEntities) throws HibernateException {
        onDelete(event);
    }
}
