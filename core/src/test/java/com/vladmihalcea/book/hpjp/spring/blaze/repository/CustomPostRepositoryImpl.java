package com.vladmihalcea.book.hpjp.spring.blaze.repository;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CriteriaBuilderFactory criteriaBuilderFactory;

    @Override
    public PagedList<Post> findTopN(Sort sortBy, int pageSize) {
        return sortedCriteriaBuilder(sortBy)
            .page(0, pageSize)
            .withKeysetExtraction(true)
            .getResultList();
    }

    @Override
    public PagedList<Post> findNextN(Sort sortBy, PagedList<Post> previousPage) {
        return sortedCriteriaBuilder(sortBy)
            .page(
                previousPage.getKeysetPage(),
                previousPage.getPage() * previousPage.getMaxResults(),
                previousPage.getMaxResults()
            )
            .getResultList();
    }

    private CriteriaBuilder<Post> sortedCriteriaBuilder(Sort sortBy) {
        CriteriaBuilder<Post> criteriaBuilder = criteriaBuilderFactory
            .create(entityManager, Post.class);
        sortBy.forEach(order -> {
            criteriaBuilder.orderBy(order.getProperty(), order.isAscending());
        });
        return criteriaBuilder;
    }
}
