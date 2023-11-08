package com.vladmihalcea.hpjp.spring.partition.service;

import com.vladmihalcea.hpjp.spring.partition.domain.Post;
import com.vladmihalcea.hpjp.spring.partition.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    private final PostRepository postRepository;

    public ForumService(
        @Autowired PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public void createPosts(List<Post> posts) {
        postRepository.persistAll(posts);
    }

    public List<Post> findByIds(List<Long> ids) {
        return postRepository.findAllById(ids);
    }

    public Post findById(Long id) {
        return postRepository.findById(id).orElse(null);
    }
}
