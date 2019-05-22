package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.service;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.dao.PostDAO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.dao.TagDAO;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
@Service
public class ForumServiceImpl implements ForumService {

    @Autowired
    private PostDAO postDAO;

    @Autowired
    private TagDAO tagDAO;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Post newPost(String title, String... tags) {
        Post post = new Post();
        post.setTitle(title);
        post.getTags().addAll(tagDAO.findByName(tags));
        return postDAO.persist(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findAllByTitle(String title) {
        List<Post> posts = postDAO.findByTitle(title);

        org.hibernate.engine.spi.PersistenceContext persistenceContext = getHibernatePersistenceContext();

        for(Post post : posts) {
            assertTrue(entityManager.contains(post));

            EntityEntry entityEntry = persistenceContext.getEntry(post);
            assertNull(entityEntry.getLoadedState());
        }

        return posts;
    }

    @Override
    @Transactional
    public Post findById(Long id) {
        Post post = postDAO.findById(id);

        org.hibernate.engine.spi.PersistenceContext persistenceContext = getHibernatePersistenceContext();

        EntityEntry entityEntry = persistenceContext.getEntry(post);
        assertNotNull(entityEntry.getLoadedState());

        return post;
    }

    private org.hibernate.engine.spi.PersistenceContext getHibernatePersistenceContext() {
        SharedSessionContractImplementor session = entityManager.unwrap(
            SharedSessionContractImplementor.class
        );
        return session.getPersistenceContext();
    }
}
