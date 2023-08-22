package com.vladmihalcea.hpjp.spring.data.masquerade.service;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.hibernate.fetching.pagination.Post_;
import com.vladmihalcea.hpjp.spring.data.masquerade.dto.PostCommentDTO;
import com.vladmihalcea.hpjp.spring.data.masquerade.dto.PostDTO;
import com.vladmihalcea.hpjp.spring.data.masquerade.repository.PostRepository;
import com.vladmihalcea.hpjp.util.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private PostRepository postRepository;

    public PagedList<PostDTO> firstLatestPosts(int pageSize) {
        return postRepository.findTopN(
            Sort.by(Post_.CREATED_ON).descending().and(Sort.by(Post_.ID).descending()), 
            pageSize
        );
    }

    public PagedList<PostDTO> findNextLatestPosts(PagedList<PostDTO> previousPage) {
        return postRepository.findNextN(
            Sort.by(Post_.CREATED_ON).descending().and(Sort.by(Post_.ID).descending()),
            previousPage
        );
    }

    public List<PostCommentDTO> findCommentsByPost(String postId) {
        return postRepository.findCommentsByPost(
            CryptoUtils.decrypt(postId, Long.class)
        );
    }
}
