package com.vladmihalcea.hpjp.spring.transaction.jta.atomikos;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.config.AtomikosJTATransactionManagerConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.dao.TagDAO;
import com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.service.ForumService;
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
@ContextConfiguration(classes = AtomikosJTATransactionManagerConfiguration.class)
public class AromikosJTATransactionManagerTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private TagDAO tagDAO;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            PostDetails.class,
            Post.class,
            Tag.class,
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                Tag hibernate = new Tag();
                hibernate.setName("hibernate");
                tagDAO.persist(hibernate);

                Tag jpa = new Tag();
                jpa.setName("jpa");
                tagDAO.persist(jpa);

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
    }
}
