package com.vladmihalcea.hpjp.spring.data.custom.repository;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.DistinctListTransformer;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTOResultTransformer;
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
    public List<PostDTO> findPostDTOWithComments() {
        return entityManager.createNativeQuery("""
            SELECT p.id AS p_id, 
                   p.title AS p_title,
                   pc.id AS pc_id, 
                   pc.review AS pc_review
            FROM post p
            JOIN post_comment pc ON p.id = pc.post_id
            ORDER BY pc.id
            """)
        .unwrap(org.hibernate.query.Query.class)
        .setTupleTransformer(new PostDTOResultTransformer())
        .setResultListTransformer(DistinctListTransformer.INSTANCE)
        .getResultList();
    }

    @Override
    public List<String> findPostTitleByTags(List<String> tags) {
        return entityManager.createNativeQuery("""
            select p.title
            from post p
            where exists (
                select 1
                from post_tag pt 
                join tag t on pt.tag_id = t.id and pt.post_id = p.id
                where t.name in (:tags)
            )
            order by p.id
            """)
            .setParameter("tags", tags)
            .getResultList();
    }

    @Override
    public void deleteAll(List<Post> posts) {
        entityManager.createQuery("""
            delete from PostComment c
            where c.post in :posts
        """)
        .setParameter("posts", posts)
        .executeUpdate();

        for(Post post : posts) {
            entityManager.remove(post);
        }
    }
}
