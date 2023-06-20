package com.vladmihalcea.hpjp.spring.blaze.service;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.hibernate.fetching.pagination.Post;
import com.vladmihalcea.hpjp.hibernate.fetching.pagination.Post_;
import com.vladmihalcea.hpjp.spring.blaze.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    @Autowired
    private PostRepository postRepository;

    public PagedList<Post> firstLatestPosts(int pageSize) {
        return postRepository.findTopN(
            Sort.by(Post_.CREATED_ON).descending().and(Sort.by(Post_.ID).descending()), 
            pageSize
        );
    }

    public PagedList<Post> findNextLatestPosts(PagedList<Post> previousPage) {
        return postRepository.findNextN(
            Sort.by(Post_.CREATED_ON).descending().and(Sort.by(Post_.ID).descending()),
            previousPage
        );
    }
}
