package com.vladmihalcea.hpjp.spring.transaction.hibernate;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.hibernate.config.HibernateTransactionManagerConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.hibernate.service.ForumService;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = HibernateTransactionManagerConfiguration.class)
public class HibernateTransactionManagerTest extends AbstractSpringTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            PostDetails.class,
            Tag.class,
            Post.class,
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                Tag hibernate = new Tag();
                hibernate.setName("hibernate");
                sessionFactory.getCurrentSession().persist(hibernate);

                Tag jpa = new Tag();
                jpa.setName("jpa");
                sessionFactory.getCurrentSession().persist(jpa);
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        Post newPost = forumService.newPost("High-Performance Java Persistence", "hibernate", "jpa");
        assertNotNull(newPost.getId());

        List<Post> posts = forumService.findAllByTitle("High-Performance Java Persistence");
        assertEquals(1, posts.size());

        Post post = forumService.findById(newPost.getId());
        assertEquals("High-Performance Java Persistence", post.getTitle());

        PostDTO postDTO = forumService.getPostDTOById(newPost.getId());
        assertEquals("High-Performance Java Persistence", postDTO.getTitle());

        //Do nothing in the transaction to check the no statement warning
        forumService.processData();
    }
}
