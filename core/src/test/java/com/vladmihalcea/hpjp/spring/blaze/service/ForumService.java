package com.vladmihalcea.hpjp.spring.blaze.service;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.spring.blaze.domain.Post;
import com.vladmihalcea.hpjp.spring.blaze.domain.Post_;
import com.vladmihalcea.hpjp.spring.blaze.domain.views.PostWithCommentsAndTagsView;
import com.vladmihalcea.hpjp.spring.blaze.repository.PostRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    private final PostRepository postRepository;

    public ForumService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

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

    public List<Post> findWithCommentsAndTagsByIds(Long minId, Long maxId) {
        return postRepository.findWithCommentsAndTagsByIds(minId, maxId);
    }

    public List<PostWithCommentsAndTagsView> findPostWithCommentsAndTagsViewByIds(Long minId, Long maxId) {
        return postRepository.findPostWithCommentsAndTagsViewByIds(minId, maxId);
    }
}
