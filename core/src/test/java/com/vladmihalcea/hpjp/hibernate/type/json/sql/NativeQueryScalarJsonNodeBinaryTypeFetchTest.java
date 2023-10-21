package com.vladmihalcea.hpjp.hibernate.type.json.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hpjp.hibernate.type.json.PostgreSQLJsonNodeBinaryTypeTest;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.hypersistence.utils.hibernate.type.json.JsonNodeBinaryType;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class NativeQueryScalarJsonNodeBinaryTypeFetchTest extends PostgreSQLJsonNodeBinaryTypeTest {

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR,
            (MetadataBuilderContributor) metadataBuilder -> metadataBuilder.applyBasicType(
                JsonNodeBinaryType.INSTANCE
            )
        );
        properties.put("hibernate.type_contributors",
            (TypeContributorList) () -> Collections.singletonList(
                (typeContributions, serviceRegistry) -> {
                    typeContributions.contributeType(new JsonBinaryType(JsonNode.class));
                }
            ));
    }

    @Test
    public void testFetchJsonProperty() {
        doInJPA(entityManager -> {
            JsonNode properties = (JsonNode) entityManager
            .createNativeQuery(
                "SELECT properties " +
                "FROM book " +
                "WHERE isbn = :isbn")
            .setParameter("isbn", "978-9730228236")
            .unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("properties", JsonNode.class)
            .getSingleResult();

            assertEquals(
                "High-Performance Java Persistence",
                properties.get("title").asText()
            );
        });
    }
}
