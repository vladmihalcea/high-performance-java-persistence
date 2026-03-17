package com.vladmihalcea.hpjp.spring.transaction.jta.camunda;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.spring.transaction.jta.service.ForumService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Camunda JavaDelegate that creates a new forum Post
 * using the existing ForumService within a JTA transaction.
 *
 * @author Vlad Mihalcea
 */
@Component("createPostDelegate")
public class CreatePostDelegate implements JavaDelegate {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private ForumService forumService;

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Creating new Post with title: {}", execution.getVariable("title"));

        String title = (String) execution.getVariable("title");

        Object tagsVar = execution.getVariable("tags");
        String[] tags;
        if (tagsVar instanceof String[]) {
            tags = (String[]) tagsVar;
        } else if (tagsVar instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> tagList = (java.util.List<String>) tagsVar;
            tags = tagList.toArray(new String[0]);
        } else {
            tags = new String[0];
        }

        Post post = forumService.newPost(title, tags);

        execution.setVariable("postId", post.getId());
    }
}

