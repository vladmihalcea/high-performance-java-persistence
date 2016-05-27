package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

/**
 * LockModeOptimisticWithPessimisticLockUpgradeTest - Test to check LockMode.OPTIMISTIC with pessimistic lock upgrade
 *
 * @author Vlad Mihalcea
 */
public class LockModeOptimisticWithPessimisticLockUpgradeTest extends LockModeOptimisticRaceConditionTest {

    @Override
    protected void lockUpgrade(EntityManager entityManager, Post post) {
        entityManager.lock(post, LockModeType.PESSIMISTIC_READ);
    }

}
