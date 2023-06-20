package com.vladmihalcea.hpjp.spring.data.query.multibag.service;

import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.multibag.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository;

    @Override
    public List<Post> findAllWithCommentsAndTags(long minId, long maxId) {
        List<Post> posts = postRepository.findAllWithComments(minId, maxId);

        return !posts.isEmpty() ?
            postRepository.findAllWithTags(minId, maxId) :
            posts;
    }
}
