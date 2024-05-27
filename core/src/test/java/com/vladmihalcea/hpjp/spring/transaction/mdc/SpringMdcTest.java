package com.vladmihalcea.hpjp.spring.transaction.mdc;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.common.domain.Post;
import com.vladmihalcea.hpjp.spring.common.domain.PostComment;
import com.vladmihalcea.hpjp.spring.common.service.ForumService;
import com.vladmihalcea.hpjp.spring.transaction.mdc.config.TransactionInfoMdcConfiguration;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = TransactionInfoMdcConfiguration.class)
public class SpringMdcTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class,
        };
    }

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
