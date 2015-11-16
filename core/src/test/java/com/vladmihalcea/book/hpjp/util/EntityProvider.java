package com.vladmihalcea.book.hpjp.util;

/**
 * <code>EntityProvider</code> - Entity Provider
 *
 * @author Vlad Mihalcea
 */
public interface EntityProvider {

    /**
     * Entity types shared among multiple test configurations
     *
     * @return entity types
     */
    Class<?>[] entities();
}
