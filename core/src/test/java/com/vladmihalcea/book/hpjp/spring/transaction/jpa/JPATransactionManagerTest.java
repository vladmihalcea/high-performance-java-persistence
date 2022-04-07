package com.vladmihalcea.book.hpjp.spring.transaction.jpa;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.book.hpjp.spring.transaction.jpa.config.JPATransactionManagerConfiguration;
import com.vladmihalcea.book.hpjp.spring.transaction.jpa.dao.PostBatchDAO;
import com.vladmihalcea.book.hpjp.spring.transaction.jpa.dao.TagDAO;
import com.vladmihalcea.book.hpjp.spring.transaction.jpa.service.ForumService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JPATransactionManagerConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JPATransactionManagerTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ForumService forumService;

    @Autowired
    private PostBatchDAO postBatchDAO;

    @Autowired
    private TagDAO tagDAO;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

                postBatchDAO.savePosts();
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
        //Check if the post was updated
        assertEquals(
            "High-Performance Java Persistence",
            entityManager.find(Post.class, post.getId()).getTitle()
        );

        PostDTO postDTO = forumService.getPostDTOById(newPost.getId());
        assertEquals("High-Performance Java Persistence", postDTO.getTitle());

        postDTO = forumService.savePostTitle(newPost.getId(), "High-Performance Java Persistence, 2nd edition");
        assertEquals("High-Performance Java Persistence, 2nd edition", postDTO.getTitle());
    }

    @Test
    public void testJdbcTemplate() {
        transactionTemplate.execute(status -> {
            int postCountBeforePersist = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Number.class).intValue();

            Post post = new Post();
            post.setTitle("Latest post!");
            entityManager.persist(post);
            entityManager.flush();

            int postCountAfterPersist = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Number.class).intValue();

            assertEquals(postCountAfterPersist, postCountBeforePersist + 1);
            return null;
        });
    }

    @Test
    public void testTransactionNoStatement() {
        transactionTemplate.execute(status -> null);
    }

    @Test
    public void testJdbcTemplateWithoutTransaction() {
        int postCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Number.class).intValue();

        transactionTemplate.execute(status -> {
            jdbcTemplate.execute("DELETE FROM post");

            return null;
        });

        int postCountAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Number.class).intValue();

        assertEquals(0, postCountAfter);
    }
}
