package com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.camunda;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.service.ForumService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
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

    @Autowired
    private ForumService forumService;

    @Override
    public void execute(DelegateExecution execution) {
        Long postId = (Long) execution.getVariable("postId");
        String title = (String) execution.getVariable("title");

        assertNotNull(postId, "Post ID should not be null");

        Post post = forumService.findById(postId);
        assertNotNull(post, "Post should be found by ID");
        assertEquals(title, post.getTitle());

        execution.setVariable("verified", true);
    }
}

