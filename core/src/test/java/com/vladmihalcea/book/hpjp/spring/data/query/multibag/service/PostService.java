package com.vladmihalcea.book.hpjp.spring.data.query.multibag.service;

import com.vladmihalcea.book.hpjp.spring.data.query.multibag.domain.Post;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface PostService {

    List<Post> findAllWithCommentsAndTags(long minId, long maxId);
}
