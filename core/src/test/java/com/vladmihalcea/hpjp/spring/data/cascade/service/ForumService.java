package com.vladmihalcea.hpjp.spring.data.cascade.service;

import com.vladmihalcea.hpjp.spring.data.cascade.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Transactional
    public PostComment addPostComment(String review, Long postId) {
        PostComment comment = new PostComment()
            .setReview(review)
            .setCreatedOn(LocalDateTime.now())
            .setPost(postRepository.getReferenceById(postId));

        postCommentRepository.persist(comment);
        return comment;
    }

    @Transactional
    public void removePostComment(Long id) {
        postCommentRepository.deleteById(id);
    }
}
