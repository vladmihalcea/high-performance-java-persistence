package com.vladmihalcea.book.hpjp.hibernate.query.hierarchical;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractTreeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            PostComment.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            PostComment root1 = new PostComment("High-Performance Java Persistence", Status.APPROVED);
            PostComment child1 = new PostComment("Is it about JDBC?", Status.APPROVED);
            PostComment child2 = new PostComment("Is it about Hibernate?", Status.APPROVED);

            PostComment child11 = new PostComment("Yes", Status.APPROVED);
            PostComment child12 = new PostComment("Yes, and not only", Status.APPROVED);

            PostComment child21 = new PostComment("Yes", Status.APPROVED);
            PostComment child22 = new PostComment("Cool!", Status.PENDING);

            PostComment root2 = new PostComment("Best of Hibernate", Status.APPROVED);

            root1.addChild(child1);
            root1.addChild(child2);

            child1.addChild(child11);
            child1.addChild(child12);

            child2.addChild(child21);
            child2.addChild(child22);

            entityManager.persist(root1);
            entityManager.persist(root2);
            entityManager.persist(child1);
            entityManager.persist(child2);
            entityManager.persist(child11);
            entityManager.persist(child12);
            entityManager.persist(child21);
            entityManager.persist(child22);
        });
    }

}
