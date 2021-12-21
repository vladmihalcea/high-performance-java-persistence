package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.data.service;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.spring.data.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
}
