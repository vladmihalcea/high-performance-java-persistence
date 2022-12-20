package com.vladmihalcea.book.hpjp.spring.transaction.mdc;

import com.vladmihalcea.book.hpjp.spring.common.domain.Post;
import com.vladmihalcea.book.hpjp.spring.common.service.ForumService;
import com.vladmihalcea.book.hpjp.spring.transaction.mdc.config.TransactionInfoMdcConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TransactionInfoMdcConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringMdcTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ForumService forumService;

    @Test
    public void testAutoMDC() {
        Post post = forumService.createPost(
            "High-Performance Java Persistence",
            "high-performance-java-persistence"
        );

        Long postId = post.getId();

        forumService.addComment(postId, "Awesome");
    }

    @Test
    public void testManualMDC() {
        try(MDC.MDCCloseable mdc = MDC
            .putCloseable(
                "txId",
                String.format(
                    " Persistence Context Id: [%d], DB Transaction Id: [%s]",
                    123456,
                    7890
                )
            )
        ) {
            LOGGER.info("Fetch Post by title");
        }
    }
}
