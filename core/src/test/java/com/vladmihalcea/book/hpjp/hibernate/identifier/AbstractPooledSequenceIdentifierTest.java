package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;

import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public abstract class AbstractPooledSequenceIdentifierTest extends AbstractTest {

    protected abstract Object newEntityInstance();

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.id.new_generator_mappings", "true");
        return properties;
    }

    protected void insertSequences() {
        LOGGER.debug("testSequenceIdentifierGenerator");
        doInJPA(entityManager -> {
            for (int i = 0; i < 5; i++) {
                entityManager.persist(newEntityInstance());
                entityManager.flush();
            }
            entityManager.flush();
            assertEquals(5, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM Post").getSingleResult()).intValue());

            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO Post VALUES NEXT VALUE FOR sequence");
                }
            });

            assertEquals(6, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM Post").getSingleResult()).intValue());
            List<Number> ids = entityManager.createNativeQuery("SELECT id FROM Post").getResultList();
            for (Number id : ids) {
                LOGGER.debug("Found id: {}", id);
            }
            for (int i = 0; i < 3; i++) {
                entityManager.persist(newEntityInstance());
                entityManager.flush();
            }
        });
    }
}
