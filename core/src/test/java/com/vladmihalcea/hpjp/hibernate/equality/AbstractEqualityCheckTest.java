package com.vladmihalcea.hpjp.hibernate.equality;

import com.vladmihalcea.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.hpjp.util.AbstractTest;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractEqualityCheckTest<T extends Identifiable<? extends Serializable>> extends AbstractTest {

    protected void assertEqualityConsistency(Class<T> clazz, T entity) {
        Set<T> tuples = new HashSet<>();
        tuples.add(entity);
        assertTrue(tuples.contains(entity));

        doInJPA(entityManager -> {
            entityManager.persist(entity);
            entityManager.flush();
            assertTrue(
                "The entity is not found in the Set after it's persisted.",
                tuples.contains(entity)
            );
        });

        assertTrue(tuples.contains(entity));

        doInJPA(entityManager -> {
            T _entity = entityManager.merge(entity);
            assertTrue(
                "The entity is not found in the Set after it's merged.",
                tuples.contains(_entity)
            );
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).update(entity);
            assertTrue(
                "The entity is not found in the Set after it's reattached.",
                tuples.contains(entity)
            );
        });

        doInJPA(entityManager -> {
            T _entity = entityManager.find(clazz, entity.getId());
            assertTrue(
                "The entity is not found in the Set after it's loaded in a different Persistence Context.",
                tuples.contains(_entity)
            );
        });

        doInJPA(entityManager -> {
            T _entity = entityManager.getReference(clazz, entity.getId());
            assertTrue(
                "The entity is not found in the Set after it's loaded as a proxy in a different Persistence Context.",
                tuples.contains(_entity)
            );
        });

        doInJPA(entityManager -> {
            T entityProxy = entityManager.getReference(
                clazz,
                entity.getId()
            );
            assertTrue(
                "The entity is not equal with the entity proxy.",
                entity.equals(entityProxy)
            );
            assertEquals(
                "The entity hashCode is different than the entity proxy.",
                entity.hashCode(),
                entityProxy.hashCode()
            );
        });

        T deletedEntity = doInJPA(entityManager -> {
            T _entity = entityManager.find(
                clazz,
                entity.getId()
            );
            entityManager.remove(_entity);
            return _entity;
        });

        assertTrue(
            "The entity is not found in the Set even after it's deleted.",
            tuples.contains(deletedEntity)
        );
    }
}
