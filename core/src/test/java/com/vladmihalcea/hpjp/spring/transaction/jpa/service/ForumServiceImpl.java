package com.vladmihalcea.hpjp.spring.transaction.jpa.service;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.hpjp.spring.transaction.jpa.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.transaction.jpa.repository.TagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@Service
public class ForumServiceImpl implements ForumService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Post newPost(String title, String... tags) {
        Post post = new Post();
        post.setTitle(title);
        post.getTags().addAll(tagRepository.findByName(tags));
        return postRepository.persist(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findAllByTitle(String title) {
        List<Post> posts = postRepository.findByTitle(title);

        org.hibernate.engine.spi.PersistenceContext persistenceContext = getHibernatePersistenceContext();

        for(Post post : posts) {
            assertTrue(entityManager.contains(post));

            EntityEntry entityEntry = persistenceContext.getEntry(post);
            assertNull(entityEntry.getLoadedState());
        }

        return posts;
    }

    @Override
    @Transactional(readOnly = true)
    public Post findById(Long id) {
        Post post = postRepository.findById(id).orElseThrow();

        org.hibernate.engine.spi.PersistenceContext persistenceContext = getHibernatePersistenceContext();

        EntityEntry entityEntry = persistenceContext.getEntry(post);
        assertNull(entityEntry.getLoadedState());

        post.setTitle(null);
        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public PostDTO getPostDTOById(Long id) {
        return postRepository.getPostDTOById(id);
    }

    @Override
    @Transactional
    public PostDTO savePostTitle(Long id, String title) {
        Post post = postRepository.findById(id).orElseThrow();

        post.setTitle(title);

        return postRepository.getPostDTOById(id);
    }

    private org.hibernate.engine.spi.PersistenceContext getHibernatePersistenceContext() {
        SharedSessionContractImplementor session = entityManager.unwrap(
            SharedSessionContractImplementor.class
        );
        return session.getPersistenceContext();
    }
}
