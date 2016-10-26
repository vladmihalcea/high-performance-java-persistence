package com.vladmihalcea.book.hpjp.hibernate.identifier.optimizer.providers;

import com.vladmihalcea.book.hpjp.util.EntityProvider;

/**
 * @author Vlad Mihalcea
 */
public abstract class PostEntityProvider<T> implements EntityProvider {

    private final Class<T> clazz;

    protected PostEntityProvider(Class<T> clazz) {
        this.clazz = clazz;
    }

    public abstract T newPost();

    @Override
    public Class<?>[] entities() {
        return new Class<?>[] {
            clazz
        };
    }
}
