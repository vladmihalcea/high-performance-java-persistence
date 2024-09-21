package com.vladmihalcea.hpjp.spring.data.custom.service;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.hpjp.spring.data.custom.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private PostRepository postRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ForumService(@Autowired PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post findById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    public List<PostDTO> findPostDTOWithComments() {
        return postRepository.findPostDTOWithComments();
    }

    @Transactional
    public void saveAntiPattern(Long postId, String postTitle) {
        Post post = postRepository.findById(postId).orElseThrow();

        post.setTitle(postTitle);

        long startNanos = System.nanoTime();
        postRepository.save(post);
        LOGGER.info("Save took: [{}] ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    @Transactional
    public Post createPost(Long id, String title) {
        return postRepository.save(
            new Post()
                .setId(id)
                .setTitle(title)
        );
    }

    @Transactional
    public void updatePostTitle(Long id, String title) {
        Post post = findById(id);
        post.setTitle(title);
    }

    @Transactional
    public void deleteAll() {
        LOGGER.info("Deleting all posts");
        entityManager.createQuery("""
            select p
            from Post p
            join fetch p.details
            join fetch p.comments
            """, Post.class)
        .getResultList();

        List<Post> posts = entityManager.createQuery("""
            select p
            from Post p
            join fetch p.tags
        """, Post.class)
        .getResultList();

        postRepository.deleteAll(posts);
    }
}
