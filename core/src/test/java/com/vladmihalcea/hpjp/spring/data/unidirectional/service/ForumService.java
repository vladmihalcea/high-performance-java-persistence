package com.vladmihalcea.hpjp.spring.data.unidirectional.service;

import com.vladmihalcea.hpjp.spring.data.unidirectional.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    private final PostRepository postRepository;

    public ForumService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public void deletePostById(Long postId) {
        postRepository.deleteById(postId);
    }
}
