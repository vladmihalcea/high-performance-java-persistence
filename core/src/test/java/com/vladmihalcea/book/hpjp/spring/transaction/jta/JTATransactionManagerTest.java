package com.vladmihalcea.book.hpjp.spring.transaction.jta;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.book.hpjp.spring.transaction.jta.config.JTATransactionManagerConfiguration;
import com.vladmihalcea.book.hpjp.spring.transaction.jta.dao.TagDAO;
import com.vladmihalcea.book.hpjp.spring.transaction.jta.service.ForumService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JTATransactionManagerConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JTATransactionManagerTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ForumService forumService;

    @Autowired
    private TagDAO tagDAO;

    @Before
    public void init() {
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
