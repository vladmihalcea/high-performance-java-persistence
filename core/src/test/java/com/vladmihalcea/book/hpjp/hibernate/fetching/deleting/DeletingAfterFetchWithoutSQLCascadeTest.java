package com.vladmihalcea.book.hpjp.hibernate.fetching.deleting;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import javax.persistence.EntityManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class DeletingAfterFetchWithoutSQLCascadeTest extends AbstractDeletingCheckTest {

    @Test
    public void testDeleteParentAfterEagerInnerFetchInChildWithoutSQLCascadeInOneTransaction() {
        createTables();
        insertData();

        doInJPA(entityManager -> {
            entityManager.createQuery("""
                SELECT a FROM Audience a
                    LEFT JOIN FETCH a.lessons l
                    LEFT JOIN FETCH l.group g
                    WHERE a.id = :audienceId
                """, Audience.class)
            .setParameter( "audienceId", 1L )
            .getSingleResult();

            Lesson lesson = entityManager.find(Lesson.class, 1L);
            entityManager.remove(lesson);
            entityManager.flush();

            try {
                Audience audience = entityManager.find(Audience.class, 1L);
                entityManager.remove(audience);
                entityManager.flush();

                fail("Should throw ConstraintViolationException");
            } catch (Exception expected) {
                assertEquals(ConstraintViolationException.class, expected.getCause().getClass());
                LOGGER.info("Failure: ", expected);
            }
        });
    }

    @Test
    public void testDeleteParentAfterEagerInnerFetchInChildWithoutSQLCascadeThenSaveAnotherEntityInSeveralTransactions() {
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
            Lesson lesson = entityManager.find(Lesson.class, 1L);
            entityManager.remove(lesson);
        }, _entityManager);

        try {
            doInJPA(entityManager -> {
                Audience audience = entityManager.find(Audience.class, 1L);
                entityManager.remove(audience);
            }, _entityManager);

            fail("Should throw ConstraintViolationException");
        } catch (Exception expected) {
            assertEquals(ConstraintViolationException.class, expected.getCause().getCause().getClass());
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
                audience_id INTEGER NOT NULL REFERENCES audiences (id),
                group_id    INTEGER NOT NULL REFERENCES groups (id)
            )
            """);
    }
}
