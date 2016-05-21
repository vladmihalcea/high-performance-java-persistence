package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

/**
 * LockModeOptimisticWithPessimisticLockUpgradeTest - Test to check LockMode.OPTIMISTIC with pessimistic lock upgrade
 *
 * @author Carol Mihalcea
 */
public class LockModeOptimisticWithPessimisticLockUpgradeTest extends LockModeOptimisticRaceConditionTest {

    @Override
    protected void lockUpgrade(Session session, Product product) {
        session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(product);
    }

}
