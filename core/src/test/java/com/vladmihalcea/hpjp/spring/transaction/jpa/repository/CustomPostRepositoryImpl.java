package com.vladmihalcea.hpjp.spring.transaction.jpa.repository;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository {

    @PersistenceContext
    private EntityManager entityManager;

    int entityCount = 10;

    @Transactional
    public void savePosts() {
        entityManager.unwrap(Session.class).setJdbcBatchSize(10);
        try {
            for ( long i = 0; i < entityCount; ++i ) {
                Post post = new Post();
                post.setTitle(String.format( "Post nr %d", i ));
                entityManager.persist( post );
            }
            entityManager.flush();
        } finally {
            entityManager.unwrap(Session.class).setJdbcBatchSize(null);
        }
    }
}
