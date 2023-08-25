package com.vladmihalcea.hpjp.spring.data.merge;

import com.vladmihalcea.hpjp.spring.data.merge.config.SpringDataJPAMergeConfiguration;
import com.vladmihalcea.hpjp.spring.data.merge.domain.Post;
import com.vladmihalcea.hpjp.spring.data.merge.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.merge.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.merge.service.ForumService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAMergeConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAMergeTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private ForumService forumService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Before
    public void init() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format("High-Performance Java Persistence, Part no. %d", i)
                        )
                        .addComment(
                            new PostComment()
                                .setReview(
                                    String.format("Part no. %d review", i)
                                )
                        )
                );
            }
            return null;
        });
    }

    @Test
    public void testSaveAll() {
        List<Post> posts = forumService.findAllByTitleLike("High-Performance Java Persistence%");

        for (Post post : posts) {
            post.setTitle("Vlad Mihalcea's " + post.getTitle());
            for (PostComment comment : post.getComments()) {
                comment.setReview(comment.getReview() + " - ⭐⭐⭐⭐⭐");
            }
        }

        forumService.saveAll(posts);
    }
}

