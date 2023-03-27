package com.vladmihalcea.book.hpjp.spring.data.query.fetch.repository;

import com.vladmihalcea.book.hpjp.spring.data.query.fetch.domain.Post;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    List<Post> findAllByTitleWithComments(String titlePattern, PageRequest pageRequest);
}
