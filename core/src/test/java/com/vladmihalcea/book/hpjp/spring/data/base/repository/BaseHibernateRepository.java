package com.vladmihalcea.book.hpjp.spring.data.base.repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface BaseHibernateRepository<T> {

    /*
        The persist methods are meant to save the newly created entities.
    */

    <S extends T> S persist(S entity);

    <S extends T> S persistAndFlush(S entity);

    <S extends T> List<S> persistAll(Iterable<S> entities);

    <S extends T> List<S> peristAllAndFlush(Iterable<S> entities);

    /*
        The merge methods are meant to propagate detached entity state changes
        if they are really needed.
    */

    <S extends T> S merge(S entity);

    <S extends T> S mergeAndFlush(S entity);

    <S extends T> List<S> mergeAll(Iterable<S> entities);

    <S extends T> List<S> mergeAllAndFlush(Iterable<S> entities);

    /*
        The update methods are meant to force the synchronization of the
        detached entity state changes.
    */

    <S extends T> S update(S entity);

    <S extends T> S updateAndFlush(S entity);

    <S extends T> List<S> updateAll(Iterable<S> entities);

    <S extends T> List<S> updateAllAndFlush(Iterable<S> entities);
}
