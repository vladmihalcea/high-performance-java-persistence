package com.vladmihalcea.book.hpjp.spring.data.custom.service;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.book.hpjp.spring.data.custom.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumServiceImpl implements ForumService {

    private PostRepository postRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ForumServiceImpl(@Autowired PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post findById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    @Override
    public List<PostDTO> findPostDTOWithComments() {
        return postRepository.findPostDTOWithComments();
    }

    @Override
    @Transactional
    public void saveAntiPattern(Long postId, String postTitle) {
        Post post = postRepository.findById(postId).orElseThrow();

        post.setTitle(postTitle);

        postRepository.save(post);
    }
}
