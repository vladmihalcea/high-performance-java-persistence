package com.vladmihalcea.hpjp.spring.data.unidirectional.service;

import com.vladmihalcea.hpjp.spring.data.unidirectional.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostDetailsRepository postDetailsRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Transactional
    public void deletePostById(Long postId) {
        postDetailsRepository.deleteById(postId);
        postCommentRepository.deleteAllByPostId(postId);
        postTagRepository.deleteAllByPostId(postId);

        postRepository.deleteById(postId);
    }
}
