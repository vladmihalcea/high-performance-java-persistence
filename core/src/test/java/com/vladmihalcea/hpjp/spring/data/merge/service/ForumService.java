package com.vladmihalcea.hpjp.spring.data.merge.service;

import com.vladmihalcea.hpjp.spring.data.merge.domain.Post;
import com.vladmihalcea.hpjp.spring.data.merge.repository.BetterPostRepository;
import com.vladmihalcea.hpjp.spring.data.merge.repository.PostRepository;
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

    @Autowired
    private BetterPostRepository betterPostRepository;

    public ForumService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> findAllByTitleLike(String titlePrefix) {
        return postRepository.findAllWithCommentsByTitleLike(titlePrefix);
    }

    @Transactional
    public void saveAll(List<Post> posts) {
        postRepository.saveAll(posts);
        //betterPostRepository.updateAll(posts);
    }
}
