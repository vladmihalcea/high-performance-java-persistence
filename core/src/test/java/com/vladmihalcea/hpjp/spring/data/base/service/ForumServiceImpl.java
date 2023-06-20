package com.vladmihalcea.hpjp.spring.data.base.service;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.spring.data.base.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumServiceImpl implements ForumService {

    private PostRepository postRepository;

    public ForumServiceImpl(@Autowired PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post findById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    @Transactional
    @Override
    public Post createPost(Post post) {
        return postRepository.persist(post);
    }

    @Transactional
    @Override
    public Post updatePost(Post post) {
        postRepository.update(post);
        return post;
    }
}
