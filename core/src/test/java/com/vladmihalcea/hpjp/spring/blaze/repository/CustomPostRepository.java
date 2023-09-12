package com.vladmihalcea.hpjp.spring.blaze.repository;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.spring.blaze.domain.Post;
import com.vladmihalcea.hpjp.spring.blaze.domain.views.PostWithCommentsAndTagsView;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    PagedList<Post> findTopN(Sort sortBy, int pageSize);

    PagedList<Post> findNextN(Sort sortBy, PagedList<Post> previousPage);

    List<Post> findWithCommentsAndTagsByIds(Long minId, Long maxId);

    List<PostWithCommentsAndTagsView> findPostWithCommentsAndTagsViewByIds(Long minId, Long maxId);
}
