package com.vladmihalcea.book.hpjp.hibernate.type.json.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonNodeBinaryType;
import com.vladmihalcea.book.hpjp.hibernate.type.json.PostgreSQLJsonNodeBinaryTypeTest;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.service.ServiceRegistry;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MetadataContributorsJsonNodeBinaryTypeFetchTest extends PostgreSQLJsonNodeBinaryTypeTest {

    @Override
    protected void additionalProperties(Properties properties) {
        TypeContributor typeContributor = new TypeContributor() {
            @Override
            public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
                typeContributions.contributeType(JsonNodeBinaryType.INSTANCE);
                typeContributions.contributeSqlTypeDescriptor(JsonNodeBinaryType.INSTANCE                       .getSqlTypeDescriptor());
            }
        };
        properties.put(
                "hibernate.type_contributors",
                (TypeContributorList) () -> Collections.singletonList(typeContributor)
        );
    }

    @Test
    @Ignore("Ignored until HHH-12925 is fixed")
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
