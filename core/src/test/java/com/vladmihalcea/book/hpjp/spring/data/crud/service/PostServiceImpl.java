package com.vladmihalcea.book.hpjp.spring.data.crud.service;

import com.vladmihalcea.book.hpjp.spring.data.crud.domain.PostComment;
import com.vladmihalcea.book.hpjp.spring.data.crud.repository.PostCommentRepository;
import com.vladmihalcea.book.hpjp.spring.data.crud.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Override
    @Transactional
    public PostComment newComment(String review, Long postId) {
        PostComment comment = new PostComment()
            .setReview(review)
            .setPost(postRepository.findById(postId).orElse(null));

        postCommentRepository.persist(comment);

        return comment;
    }
}
