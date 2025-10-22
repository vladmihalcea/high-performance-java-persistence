package com.vladmihalcea.hpjp.spring.data.dto2entity.service;

import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.Post;
import com.vladmihalcea.hpjp.spring.data.dto2entity.dto.PostDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public class ForumFacadeService {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private ForumService forumService;

    public void updatePost(PostDTO postDTO) {
        LOGGER.info("Convert DTO to JPA entity");
        Post post = forumService.convertDTOToPost(postDTO);
        LOGGER.info("Merge updated JPA entity");
        forumService.mergePost(post);
    }
}
