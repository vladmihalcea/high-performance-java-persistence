package com.vladmihalcea.hpjp.spring.data.query.multibag.repository;

import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Post;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository {
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Post> findAllWithCommentsAndTags(long minId, long maxId) {
        return entityManager.createQuery("""
            select p
            from Post p
            left join fetch p.comments
            left join fetch p.tags
            where p.id between :minId and :maxId
            """, Post.class)
        .setParameter("minId", minId)
        .setParameter("maxId", maxId)
        .getResultList();
    }
}
