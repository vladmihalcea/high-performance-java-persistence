package com.vladmihalcea.book.hpjp.spring.data.query.fetch.repository;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.vladmihalcea.book.hpjp.spring.data.query.fetch.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.query.fetch.domain.Post_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CriteriaBuilderFactory criteriaBuilderFactory;

    @Override
    public List<Post> findAllByTitleWithComments(String titlePattern, PageRequest pageRequest) {
        return criteriaBuilderFactory
            .create(entityManager, Post.class)
            .fetch(Post_.COMMENTS)
            .where(Post_.TITLE).like().value(titlePattern).noEscape()
            .orderBy(Post_.CREATED_ON, true)
            .setFirstResult((int) pageRequest.getOffset())
            .setMaxResults(pageRequest.getPageSize())
            .getResultList();
    }
}
