package com.vladmihalcea.hpjp.spring.transaction.jta.camunda;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.spring.transaction.jta.service.ForumService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Camunda JavaDelegate that verifies the Post was created
 * successfully within the same JTA transaction.
 *
 * @author Vlad Mihalcea
 */
@Component("verifyPostDelegate")
public class VerifyPostDelegate implements JavaDelegate {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private ForumService forumService;

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Verifying Post with ID: {}", execution.getVariable("postId"));

        Long postId = (Long) execution.getVariable("postId");
        String title = (String) execution.getVariable("title");

        assertNotNull(postId, "Post ID should not be null");

        Post post = forumService.findById(postId);
        assertNotNull(post, "Post should be found by ID");
        assertEquals(title, post.getTitle());

        execution.setVariable("verified", true);
    }
}

