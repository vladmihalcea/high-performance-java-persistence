package com.vladmihalcea.hpjp.spring.data.query.multibag.service;

import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Post;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface BrokenPostService {

    List<Post> findAllWithCommentsAndTags(long minId, long maxId);
}
