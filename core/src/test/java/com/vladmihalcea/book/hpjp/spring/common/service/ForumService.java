package com.vladmihalcea.book.hpjp.spring.common.service;

import com.vladmihalcea.book.hpjp.spring.common.domain.Post;
import com.vladmihalcea.book.hpjp.spring.common.repository.PostRepository;
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

    public Post createPost(String title, String slug) {
        return postRepository.persist(
            new Post()
                .setTitle(title)
                .setSlug(slug)
        );
    }

    public Post findBySlug(String slug){
        return postRepository.findBySlug(slug);
    }
}
