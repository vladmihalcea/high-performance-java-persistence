package com.vladmihalcea.book.hpjp.hibernate.type.json.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.hibernate.type.json.PostgreSQLJsonNodeBinaryTypeTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class DefaultPostgreSQLJsonNodeBinaryTypeFetchTest extends PostgreSQLJsonNodeBinaryTypeTest {

    @Test
    public void testFetchJsonPropertyUsingNativeSQL() {

        try {
            doInJPA(entityManager -> {
                JsonNode properties = (JsonNode) entityManager
                .createNativeQuery(
                    "SELECT properties " +
                    "FROM book " +
                    "WHERE isbn = :isbn")
                .setParameter("isbn", "978-9730228236")
                .getSingleResult();

                assertEquals("High-Performance Java Persistence", properties.get("title").asText());
            });

            fail("Should throw exception!");
        } catch (Exception e) {
            LOGGER.error("Failure", e);

            Exception rootCause = ExceptionUtil.rootCause(e);
            assertEquals("No Dialect mapping for JDBC type: 1111", rootCause.getMessage());
        }
    }
}
