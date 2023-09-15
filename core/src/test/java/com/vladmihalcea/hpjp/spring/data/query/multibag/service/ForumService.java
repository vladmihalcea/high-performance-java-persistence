package com.vladmihalcea.hpjp.spring.data.query.multibag.service;

import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.multibag.repository.PostCommentRepository;
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
public class ForumService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    public List<PostComment> findAllCommentsByReview(String review) {
        return postCommentRepository.findAllByReview(review);
    }

    public List<Post> findAllWithCommentsAndTags(Long minId, Long maxId) {
        List<Post> posts = postRepository.findAllWithComments(minId, maxId);

        return !posts.isEmpty() ?
            postRepository.findAllWithTags(minId, maxId) :
            posts;
    }
}
