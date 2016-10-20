package com.vladmihalcea.book.hpjp.hibernate.equality;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractEqualityCheckTest extends AbstractTest {

    protected <T extends Identifiable<? extends Serializable>> void assertEqualityConstraints(Class<T> clazz, T entity) {
        Set<T> tuples = new HashSet<>();

        assertFalse(tuples.contains(entity));
        tuples.add(entity);
        assertTrue(tuples.contains(entity));

        doInJPA(entityManager -> {
            entityManager.persist(entity);
            entityManager.flush();
            assertTrue("The entity is found after it's persisted",
                tuples.contains(entity));
        });

        //The entity is found after the entity is detached
        assertTrue(tuples.contains(entity));

        doInJPA(entityManager -> {
            T _entity = entityManager.merge(entity);
            assertTrue("The entity is found after it's merged",
                tuples.contains(_entity));
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).update(entity);
            assertTrue("The entity is found after it's reattached",
                tuples.contains(entity));
        });

        doInJPA(entityManager -> {
            T _entity = entityManager.find(clazz, entity.getId());
            assertTrue("The entity is found after it's loaded " +
                       "in an other Persistence Context",
                tuples.contains(_entity));
        });

        doInJPA(entityManager -> {
            T _entity = entityManager.getReference(clazz, entity.getId());
            assertTrue("The entity is found after it's loaded as a Proxy " +
                            "in an other Persistence Context",
            tuples.contains(_entity));
        });

        executeSync(() -> {
            doInJPA(entityManager -> {
                T _entity = entityManager.find(clazz, entity.getId());
                assertTrue("The entity is found after it's loaded " +
                           "in an other Persistence Context and " +
                           "in an other thread",
                    tuples.contains(_entity));
            });
        });
    }
}
