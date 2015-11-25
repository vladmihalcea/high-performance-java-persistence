package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.TaskEntityProvider;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.Post;
import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.PostComment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
