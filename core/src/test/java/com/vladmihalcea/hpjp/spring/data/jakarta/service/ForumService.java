package com.vladmihalcea.hpjp.spring.data.jakarta.service;

import com.vladmihalcea.hpjp.spring.data.jakarta.domain.Post;
import com.vladmihalcea.hpjp.spring.data.jakarta.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.jakarta.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.jakarta.repository.PostRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    @Inject
    private PostRepository postRepository;

    @Inject
    private PostCommentRepository postCommentRepository;

    public List<PostComment> findCommentsByReview(String review) {
        return postCommentRepository.findByReview(review);
    }

    @Transactional
    public Post addPost(String title) {
        Post post = new Post();
        post.setTitle(title);

        return postRepository.persist(post);
    }

    @Transactional
    public PostComment addPostComment(String review, Long postId) {
        PostComment comment = new PostComment()
            .setReview(review)
            .setCreatedOn(LocalDateTime.now())
            .setPost(new Post().setId(postId));

        postCommentRepository.persist(comment);
        return comment;
    }

    @Transactional
    public void updateComment(PostComment comment) {
        postCommentRepository.save(comment);
    }

    @Transactional
    public void removePostComment(Long id) {
        postCommentRepository.deleteById(id);
    }
}
