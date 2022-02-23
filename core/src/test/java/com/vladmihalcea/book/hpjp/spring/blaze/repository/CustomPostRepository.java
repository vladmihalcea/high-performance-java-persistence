package com.vladmihalcea.book.hpjp.spring.blaze.repository;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.Post;
import org.springframework.data.domain.Sort;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    PagedList<Post> findTopN(Sort sortBy, int pageSize);

    PagedList<Post> findNextN(Sort sortBy, PagedList<Post> previousPage);
}
