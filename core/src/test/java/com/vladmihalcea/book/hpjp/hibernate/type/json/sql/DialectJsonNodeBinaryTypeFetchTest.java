package com.vladmihalcea.book.hpjp.hibernate.type.json.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.hibernate.type.json.PostgreSQLJsonNodeBinaryTypeTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DialectJsonNodeBinaryTypeFetchTest extends PostgreSQLJsonNodeBinaryTypeTest {

    @Test
    public void testFetchJsonProperty() {
        doInJPA(entityManager -> {
            JsonNode properties = (JsonNode) entityManager
            .createNativeQuery(
                "SELECT properties " +
                "FROM book " +
                "WHERE isbn = :isbn")
            .setParameter("isbn", "978-9730228236")
            .getSingleResult();

            assertEquals(
                "High-Performance Java Persistence",
                properties.get("title").asText()
            );
        });
    }
}
