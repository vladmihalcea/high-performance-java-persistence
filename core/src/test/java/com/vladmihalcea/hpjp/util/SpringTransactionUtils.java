package com.vladmihalcea.hpjp.util;

import jakarta.persistence.EntityManager;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <code>SpringTransactionUtils</code> - Spring Transaction utilities holder.
 *
 * @author Vlad Mihalcea
 */
public final class SpringTransactionUtils {

    private SpringTransactionUtils() {
        throw new UnsupportedOperationException("SpringTransactionUtils is not instantiable!");
    }

    /**
     * Return the current {@link EntityManager} instance bound to the current running
     * transaction.
     *
     * @return current {@link EntityManager}
     */
    public static EntityManager currentEntityManager() {
        return TransactionSynchronizationManager.getResourceMap()
            .values()
            .stream()
            .filter(EntityManagerHolder.class::isInstance)
            .map(eh -> ((EntityManagerHolder) eh).getEntityManager())
            .findAny()
            .orElse(null);
    }
}
