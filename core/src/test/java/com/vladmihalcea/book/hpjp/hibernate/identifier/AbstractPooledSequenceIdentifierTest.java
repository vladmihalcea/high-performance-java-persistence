package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public abstract class AbstractPooledSequenceIdentifierTest extends AbstractTest {

    protected abstract Object newEntityInstance();

    @Override
    protected Properties properties() {
        Properties properties = properties();
        properties.put("hibernate.id.new_generator_mappings", "true");
        return properties;
    }

    protected void insertSequences() {
        LOGGER.debug("testSequenceIdentifierGenerator");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            for (int i = 0; i < 8; i++) {
                entityManager.persist(newEntityInstance());
            }
            entityManager.flush();
            assertEquals(8, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM sequenceIdentifier").getSingleResult()).intValue());
            insertNewRow(session);
            insertNewRow(session);
            insertNewRow(session);
            assertEquals(11, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM sequenceIdentifier").getSingleResult()).intValue());
            List<Number> ids = entityManager.createNativeQuery("SELECT id FROM sequenceIdentifier", Number.class).getResultList();
            for (Number id : ids) {
                LOGGER.debug("Found id: {}", id);
            }
            for (int i = 0; i < 3; i++) {
                entityManager.persist(newEntityInstance());
            }
            entityManager.flush();
        });
    }

    private void insertNewRow(Session session) {
        session.doWork(connection -> {
            try(Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO sequenceIdentifier VALUES NEXT VALUE FOR hibernate_sequence");
            }
        });
    }
}
