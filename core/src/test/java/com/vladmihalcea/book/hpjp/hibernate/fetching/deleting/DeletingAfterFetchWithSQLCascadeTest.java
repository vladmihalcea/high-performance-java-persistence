package com.vladmihalcea.book.hpjp.hibernate.fetching.deleting;

import org.hibernate.TransientPropertyValueException;
import org.junit.Test;

import javax.persistence.EntityManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class DeletingAfterFetchWithSQLCascadeTest extends AbstractDeletingCheckTest {

    @Test
    public void testDeleteParentAfterEagerInnerFetchInChildWithSQLCascadeChildInOneTransaction() {
        createTables();
        insertData();

        try {
            doInJPA(entityManager -> {
                entityManager.createQuery("""
                    SELECT a FROM Audience a
                        LEFT JOIN FETCH a.lessons l
                        LEFT JOIN FETCH l.group g
                        WHERE a.id = :audienceId
                    """, Audience.class)
                .setParameter( "audienceId", 1L )
                .getSingleResult();

                Audience audience = entityManager.find(Audience.class, 1L);
                entityManager.remove(audience);
                entityManager.flush();
            });

            fail("Should throw TransientPropertyValueException");
        } catch (Exception expected) {
            assertEquals(TransientPropertyValueException.class, expected.getCause().getCause().getClass());
            LOGGER.info("Failure: ", expected);
        }
    }

    @Test
    public void testDeleteParentAfterEagerInnerFetchInChildWithSQLCascadeThenSaveAnotherEntityInSeveralTransactions() {
        createTables();
        insertData();

        EntityManager _entityManager = entityManagerFactory().createEntityManager();

        doInJPA(entityManager -> {
            entityManager.createQuery("""
                SELECT a FROM Audience a
                    LEFT JOIN FETCH a.lessons l
                    LEFT JOIN FETCH l.group g
                    WHERE a.id = :audienceId
                """, Audience.class)
            .setParameter( "audienceId", 1L )
            .getSingleResult();
        }, _entityManager);

        doInJPA(entityManager -> {
            Audience audience = entityManager.find(Audience.class, 1L);
            entityManager.remove(audience);
        }, _entityManager);

        try {
            doInJPA(entityManager -> {
                Audience audience = entityManager.find(Audience.class, 2L);
                entityManager.merge(audience);
            }, _entityManager);

            fail("Should throw TransientPropertyValueException");
        } catch (Exception expected) {
            assertEquals(TransientPropertyValueException.class, expected.getCause().getCause().getClass());
            LOGGER.info("Failure: ", expected);
        } finally {
            _entityManager.close();
        }
    }

    @Override
    protected void createTables() {
        super.createTables();
        executeStatement("""
            CREATE TABLE lessons(
                id          BIGINT PRIMARY KEY,
                audience_id INTEGER NOT NULL REFERENCES audiences (id) ON DELETE CASCADE,
                group_id    INTEGER NOT NULL REFERENCES groups (id) ON DELETE CASCADE
            )
            """);
    }
}
