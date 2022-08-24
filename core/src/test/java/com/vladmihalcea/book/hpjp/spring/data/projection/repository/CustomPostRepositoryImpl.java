package com.vladmihalcea.book.hpjp.spring.data.projection.repository;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTOResultTransformer;
import org.springframework.data.repository.query.Param;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository {
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PostDTO> findPostDTOByTitle(@Param("postTitle") String postTitle) {
        return entityManager.createNativeQuery("""
            SELECT p.id AS p_id, 
                   p.title AS p_title,
                   pc.id AS pc_id, 
                   pc.review AS pc_review
            FROM post p
            JOIN post_comment pc ON p.id = pc.post_id
            WHERE p.title LIKE :postTitle
            ORDER BY pc.id
            """)
        .setParameter("postTitle", postTitle)
        .unwrap(org.hibernate.query.Query.class)
        .setResultTransformer(new PostDTOResultTransformer())
        .getResultList();
    }
}
