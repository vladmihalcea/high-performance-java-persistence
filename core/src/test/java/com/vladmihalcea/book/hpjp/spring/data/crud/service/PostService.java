package com.vladmihalcea.book.hpjp.spring.data.crud.service;

import com.vladmihalcea.book.hpjp.spring.data.crud.domain.PostComment;
import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface PostService {

    PostComment addNewPostComment(String review, Long postId);
}
