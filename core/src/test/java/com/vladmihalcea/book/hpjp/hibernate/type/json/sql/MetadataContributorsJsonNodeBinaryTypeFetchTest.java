package com.vladmihalcea.book.hpjp.hibernate.type.json.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.hibernate.type.json.JacksonUtil;
import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonNodeBinaryType;
import com.vladmihalcea.book.hpjp.hibernate.type.json.PostgreSQLJsonNodeBinaryTypeTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.*;
import java.util.Arrays;
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
        properties.put( "hibernate.type_contributors", (TypeContributorList) () -> Collections.singletonList(
                (typeContributions, serviceRegistry) ->
                    typeContributions.contributeSqlTypeDescriptor(JsonNodeBinaryType.INSTANCE.getSqlTypeDescriptor())
        ));
    }

    @Test
    //@Ignore
    public void testFetchJsonProperty() {
        doInJPA(entityManager -> {
            List<?> properties = entityManager.createNativeQuery(
                "select properties from book")
            .getResultList();

            assertEquals(1, properties.size());
        });
    }
}
