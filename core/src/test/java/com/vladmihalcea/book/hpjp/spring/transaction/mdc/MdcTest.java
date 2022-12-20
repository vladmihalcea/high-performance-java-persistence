package com.vladmihalcea.book.hpjp.spring.transaction.mdc;

import com.vladmihalcea.book.hpjp.spring.common.domain.Post;
import com.vladmihalcea.book.hpjp.spring.common.service.ForumService;
import com.vladmihalcea.book.hpjp.spring.transaction.mdc.config.MdcConfiguration;
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

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MdcConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MdcTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ForumService forumService;

    @Before
    public void init() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                forumService.createPost(
                    "High-Performance Java Persistence",
                    "high-performance-java-persistence"
                );
                
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = forumService.findBySlug("high-performance-java-persistence");

            assertEquals(
                "High-Performance Java Persistence",
                post.getTitle()
            );
            
            return null;
        });
    }
}
