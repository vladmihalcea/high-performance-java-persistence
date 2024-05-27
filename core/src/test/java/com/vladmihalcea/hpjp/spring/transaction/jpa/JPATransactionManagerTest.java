package com.vladmihalcea.hpjp.spring.transaction.jpa;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.jpa.config.JPATransactionManagerConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.jpa.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.transaction.jpa.repository.TagRepository;
import com.vladmihalcea.hpjp.spring.transaction.jpa.service.ForumService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = JPATransactionManagerConfiguration.class)
public class JPATransactionManagerTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

                postRepository.savePosts();
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
