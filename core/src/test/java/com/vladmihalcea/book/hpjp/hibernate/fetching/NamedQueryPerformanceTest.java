package com.vladmihalcea.book.hpjp.hibernate.fetching;

import org.hibernate.Session;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * @author Vlad Mihalcea
 */
public class NamedQueryPerformanceTest extends PlanCacheSizePerformanceTest {

    public static final String QUERY_NAME_1 = "findPostCommentSummary";
    public static final String QUERY_NAME_2 = "findPostComments";

    public NamedQueryPerformanceTest(int planCacheMaxSize) {
        super(planCacheMaxSize);
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManagerFactory().addNamedQuery(QUERY_NAME_1, getEntityQuery1(entityManager));
            entityManagerFactory().addNamedQuery(QUERY_NAME_2, getEntityQuery2(entityManager));
        });
    }

    @Override
    protected Query getEntityQuery1(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        return session.getNamedQuery(QUERY_NAME_1);
    }

    @Override
    protected Query getEntityQuery2(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        return session.getNamedQuery(QUERY_NAME_2);
    }
}
