package com.vladmihalcea.hpjp.hibernate.identifier;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.Session;

import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public abstract class AbstractPooledSequenceIdentifierTest extends AbstractTest {

    protected abstract Object newEntityInstance();

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

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
            assertEquals(
                5,
                ((Number) entityManager.createNativeQuery("""
                    SELECT COUNT(*) 
                    FROM post
                    """
                ).getSingleResult()).intValue()
            );

            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                        INSERT INTO post (
                            id, 
                            title
                        ) 
                        VALUES (
                            nextval('post_sequence'), 
                            'High-Performance Hibernate'
                        )
                        """);
                }
            });

            assertEquals(
                6,
                ((Number) entityManager.createNativeQuery("""
                    SELECT COUNT(*) 
                    FROM post
                    """
                ).getSingleResult()).intValue()
            );
            List<Number> ids = entityManager.createNativeQuery("SELECT id FROM post").getResultList();
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
