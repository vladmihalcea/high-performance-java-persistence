package com.vladmihalcea.book.hpjp.spring.data.crud.service;

import com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.book.hpjp.spring.data.crud.domain.PostComment;
import com.vladmihalcea.book.hpjp.spring.data.crud.repository.PostCommentRepository;
import com.vladmihalcea.book.hpjp.spring.data.crud.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

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
    public PostComment addNewPostComment(String review, Long postId) {
        PostComment comment = new PostComment()
            .setReview(review)
            .setPost(postRepository.findById(postId).orElseThrow(
                ()-> new EntityNotFoundException(
                    String.format("Post with id [%d] was not found!", postId)
                )
            ));

        postCommentRepository.save(comment);

        return comment;
    }
}
