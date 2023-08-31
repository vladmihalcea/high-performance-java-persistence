package com.vladmihalcea.hpjp.spring.data.projection.repository;

import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.DistinctListTransformer;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTOResultTransformer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository {
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PostDTO> findPostDTOByPostTitle(@Param("postTitle") String postTitle) {
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
        .unwrap(Query.class)
        .setTupleTransformer(new PostDTOResultTransformer())
        .setResultListTransformer(DistinctListTransformer.INSTANCE)
        .getResultList();
    }
}
