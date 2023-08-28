package com.vladmihalcea.hpjp.spring.data.query.fetch.service;

import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.fetch.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Vlad Mihalcea
 */
@Service
public class ForumService {

    @Autowired
    private PostRepository postRepository;

    @Transactional(readOnly = true)
    public List<Post> findAllPostsByTitleWithComments(String titlePattern, PageRequest pageRequest) {
        return postRepository.findAllByIdWithComments(
            postRepository.findAllPostIdsByTitle(
                titlePattern,
                pageRequest
            )
        );
    }

    @Transactional(readOnly = true)
    public List<Post> findAllPostsPublishedToday() {
        try(Stream stream = postRepository.streamByCreatedOnSince(LocalDate.now())) {
            return stream.toList();
        }
    }
}
