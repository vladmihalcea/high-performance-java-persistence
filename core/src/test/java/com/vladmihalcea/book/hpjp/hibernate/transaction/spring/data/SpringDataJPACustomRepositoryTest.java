package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.data;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.spring.data.config.SpringDataJPACustomRepositoryConfiguration;
import com.vladmihalcea.book.hpjp.hibernate.transaction.spring.data.service.ForumService;
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

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPACustomRepositoryConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPACustomRepositoryTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ForumService forumService;

    @Before
    public void init() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                entityManager.persist(
                    new Post()
                        .setId(1L)
                        .setTitle("High-Performance Java Persistence")
                        .addComment(
                            new PostComment()
                                .setId(1L)
                                .setReview("Best book on JPA and Hibernate!")
                        )
                        .addComment(
                            new PostComment()
                                .setId(2L)
                                .setReview("A must-read for every Java developer!")
                        )
                );


                entityManager.persist(
                    new Post()
                        .setId(2L)
                        .setTitle("Hypersistence Optimizer")
                        .addComment(
                            new PostComment()
                                .setId(3L)
                                .setReview("It's like pair programming with Vlad!")
                        )
                );
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        List<PostDTO> postDTOs = forumService.findPostDTOWithComments();

        assertEquals(2, postDTOs.size());
        assertEquals(2, postDTOs.get(0).getComments().size());
        assertEquals(1, postDTOs.get(1).getComments().size());

        PostDTO post1DTO = postDTOs.get(0);

        assertEquals(1L, post1DTO.getId().longValue());
        assertEquals(2, post1DTO.getComments().size());
        assertEquals(1L, post1DTO.getComments().get(0).getId().longValue());
        assertEquals(2L, post1DTO.getComments().get(1).getId().longValue());

        PostDTO post2DTO = postDTOs.get(1);

        assertEquals(2L, post2DTO.getId().longValue());
        assertEquals(1, post2DTO.getComments().size());
        assertEquals(3L, post2DTO.getComments().get(0).getId().longValue());
    }
}

