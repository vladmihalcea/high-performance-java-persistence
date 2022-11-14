package com.vladmihalcea.book.hpjp.spring.data.save.service;

import com.vladmihalcea.book.hpjp.spring.data.save.domain.PostComment;
import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface PostService {

    PostComment newComment(String review, Long postId);
}
