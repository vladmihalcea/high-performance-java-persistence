package com.vladmihalcea.book.hpjp.hibernate.type.json.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.hibernate.type.json.PostgreSQLJsonNodeBinaryTypeTest;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MetadataContributorsJsonNodeBinaryTypeFetchTest extends PostgreSQLJsonNodeBinaryTypeTest {

    @Override
    protected void additionalProperties(Properties properties) {
        TypeContributor typeContributor = (typeContributions, serviceRegistry) -> {
            typeContributions.contributeType(JsonNodeBinaryType.INSTANCE);
        };
        properties.put(
            "hibernate.type_contributors",
            (TypeContributorList) () -> Collections.singletonList(typeContributor)
        );
    }

    @Test
    public void testFetchJsonProperty() {
        doInJPA(entityManager -> {
            JsonNode properties = (JsonNode) entityManager.createNativeQuery(
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
