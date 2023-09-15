package com.vladmihalcea.hpjp.spring.data.query.multibag.service;

import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.multibag.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class BrokenForumService {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private PostRepository postRepository;

    public List<Post> findAllWithCommentsAndTags(long minId, long maxId) {
        return postRepository.findAllWithCommentsAndTags(minId, maxId);
    }
}
