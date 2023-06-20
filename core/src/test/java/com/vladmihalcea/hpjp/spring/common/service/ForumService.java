package com.vladmihalcea.hpjp.spring.common.service;

import com.vladmihalcea.hpjp.spring.common.domain.Post;
import com.vladmihalcea.hpjp.spring.common.domain.PostComment;
import com.vladmihalcea.hpjp.spring.common.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.common.repository.PostRepository;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    private final PostRepository postRepository;

    private final PostCommentRepository postCommentRepository;

    public ForumService(
            PostRepository postRepository,
            PostCommentRepository postCommentRepository) {
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
    }

    @Transactional
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

    @Transactional
    public void updatePostTitle(String slug, String title) {
        Post post = findBySlug(slug);
        post.setTitle(title);
        postRepository.flush();
    }

    @Transactional
    public void addComment(Long postId, String review) {
        Post post = postRepository.lockById(postId, LockModeType.OPTIMISTIC);

        postCommentRepository.persist(
            new PostComment()
                .setReview(review)
                .setPost(post)
        );

        postRepository.lockById(postId, LockModeType.PESSIMISTIC_READ);
    }
}
