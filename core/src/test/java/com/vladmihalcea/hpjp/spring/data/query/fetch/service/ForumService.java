package com.vladmihalcea.hpjp.spring.data.query.fetch.service;

import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.fetch.repository.PostRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * @author Vlad Mihalcea
 */
@Service
public class ForumService {

    private final PostRepository postRepository;

    private final CacheManager cacheManager;

    private final Cache postCache;

    private final ExecutorService executorService;

    public ForumService(
            PostRepository postRepository,
            CacheManager cacheManager,
            ExecutorService executorService) {
        this.postRepository = postRepository;
        this.cacheManager = cacheManager;
        this.postCache = cacheManager.getCache(Post.class.getSimpleName());
        this.executorService = executorService;
    }

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
        try(Stream<Post> stream = postRepository.streamByCreatedOnSince(LocalDate.now())) {
            return stream.toList();
        }
    }

    @Transactional(readOnly = true)
    public void updatePostCache() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try(Stream<Post> postStream = postRepository.streamByCreatedOnSince(yesterday)) {
            postStream.forEach(post -> executorService.submit(() -> postCache.put(post.getId(), post)));
        }
    }
}
