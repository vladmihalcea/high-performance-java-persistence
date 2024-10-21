package com.vladmihalcea.hpjp.spring.data.bidirectional.service;

import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.Post;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.bidirectional.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.bidirectional.repository.PostRepository;
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
    private PostCommentRepository postCommentRepository;

    @Transactional
    public PostComment addPostComment(String review, Long postId) {
        PostComment comment = new PostComment()
            .setReview(review)
            .setPost(postRepository.getReferenceById(postId));

        postCommentRepository.persist(comment);
        return comment;
    }

    @Transactional
    public void removePostComment(Long id) {
        postCommentRepository.deleteById(id);
    }

    @Transactional
    public PostComment addPostCommentAntiPattern(String review, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();

        PostComment comment = new PostComment()
            .setReview(review)
            .setPost(postRepository.getReferenceById(postId));

        post.addComment(comment);
        return comment;
    }

    @Transactional
    public void removePostCommentAntiPattern(Long id) {
        PostComment postComment = postCommentRepository.findById(id).orElseThrow();
        postComment.getPost().removeComment(postComment);
    }
}
