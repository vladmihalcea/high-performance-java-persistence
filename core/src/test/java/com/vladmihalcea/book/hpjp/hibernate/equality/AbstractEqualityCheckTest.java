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
public abstract class AbstractEqualityCheckTest<T extends Identifiable<? extends Serializable>> extends AbstractTest {

    protected void assertEqualityConsistency(
            Class<T> clazz,
            T entity) {

        Set<T> tuples = new HashSet<>();

        assertFalse(tuples.contains(entity));
        tuples.add(entity);
        assertTrue(tuples.contains(entity));

        doInJPA(entityManager -> {

            /**
             * someone creates a DAO with a general way of putting both transient and detached objects in managed state, implementing it with entityManager.merge.
             * For putting transient objects to managed state this means there is an object change: the returned object should be used for further logic.
             */

            T _entity = entityManager.merge(entity); //  instead of entityManager.persist(entity);
            entityManager.flush();

            assertTrue(
                    "The entity is not found in the Set after it's persisted. " +
                            "This is because in the equals method it is not the same object (since merge returns a new object) and the id is not the same (since the transient object does not have one)",
                    tuples.contains(_entity)
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
                    "The entity is not found in the Set after it's loaded in a subsequent Persistence Context.",
                    tuples.contains(_entity)
            );
        });

        doInJPA(entityManager -> {
            T _entity = entityManager.getReference(clazz, entity.getId());
            assertTrue(
                    "The entity is not in the Set found after it's loaded as a proxy in an other Persistence Context.",
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
                    entity.equals(entityProxy));
        });

        T deletedEntity = doInJPA(entityManager -> {
            T _entity = entityManager.getReference(
                    clazz,
                    entity.getId()
            );
            entityManager.remove(_entity);
            return _entity;
        });

        assertTrue(
                "The entity is found in not the Set even after it's deleted.",
                tuples.contains(deletedEntity)
        );
    }
}
