package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.TaskEntityProvider;
import org.junit.Test;

/**
 * EntityGraphMapperTest - Test mapping to entity
 *
 * @author Vlad Mihalcea
 */
public class TaskTypingTest extends AbstractMySQLIntegrationTest {

    private TaskEntityProvider entityProvider = new TaskEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testJdbcOneToManyMapping() {
        doInJDBC(connection -> {

        });
    }
}
