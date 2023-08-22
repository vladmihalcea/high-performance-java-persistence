package com.vladmihalcea.hpjp.spring.data.masquerade.repository;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.spring.data.masquerade.domain.Post;
import com.vladmihalcea.hpjp.spring.data.masquerade.dto.PostDTO;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;

/**
 * @author Vlad Mihalcea
 */
public class CustomPostRepositoryImpl implements CustomPostRepository {

    private final EntityManager entityManager;

    private final CriteriaBuilderFactory criteriaBuilderFactory;

    public CustomPostRepositoryImpl(
            EntityManager entityManager,
            CriteriaBuilderFactory criteriaBuilderFactory) {
        this.entityManager = entityManager;
        this.criteriaBuilderFactory = criteriaBuilderFactory;
    }

    @Override
    public PagedList<PostDTO> findTopN(Sort sortBy, int pageSize) {
        return sortedCriteriaBuilder(sortBy)
            .page(0, pageSize)
            .withKeysetExtraction(true)
            .getResultList();
    }

    @Override
    public PagedList<PostDTO> findNextN(Sort sortBy, PagedList<PostDTO> previousPage) {
        return sortedCriteriaBuilder(sortBy)
            .page(
                previousPage.getKeysetPage(),
                previousPage.getPage() * previousPage.getMaxResults(),
                previousPage.getMaxResults()
            )
            .getResultList();
    }

    private CriteriaBuilder<PostDTO> sortedCriteriaBuilder(Sort sortBy) {
        CriteriaBuilder<Post> criteriaBuilder = criteriaBuilderFactory
            .create(entityManager, Post.class)
            .from(Post.class, "p");
        sortBy.forEach(order -> {
            criteriaBuilder.orderBy(order.getProperty(), order.isAscending());
        });
        return criteriaBuilder.selectNew(PostDTO.class)
            .with("p.id")
            .with("p.title")
            .end();
    }
}
