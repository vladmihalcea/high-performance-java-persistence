package com.vladmihalcea.book.hpjp.spring.transaction.jpa.dao;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * @author Vlad Mihalcea
 */
@Repository
public class PostBatchDAOImpl extends GenericDAOImpl<Post, Long> implements PostBatchDAO {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    int entityCount = 10;

    protected PostBatchDAOImpl() {
        super(Post.class);
    }

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
