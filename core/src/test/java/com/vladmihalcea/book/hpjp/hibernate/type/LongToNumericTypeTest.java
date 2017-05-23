package com.vladmihalcea.book.hpjp.hibernate.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.PersistenceUnitInfoImpl;

/**
 * @author Vlad Mihalcea
 */
public class LongToNumericTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Event.class
        };
    }

    /*protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.hbm2ddl.auto", "validate");
        return properties;
    }*/

    protected Properties validateProperties() {
        Properties properties = super.properties();
        properties.put("hibernate.hbm2ddl.auto", "validate");
        return properties;
    }

    @Test
    public void test() {
        PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl(
                LongToNumericTypeTest.class.getName(),
                Collections.singletonList( ValidateEvent.class.getName() ),
                validateProperties()
        );

        Map<String, Object> configuration = new HashMap<>();
        EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = new EntityManagerFactoryBuilderImpl(
                new PersistenceUnitInfoDescriptor( persistenceUnitInfo), configuration
        );
        EntityManagerFactory emf = null;
        try {
            emf = entityManagerFactoryBuilder.build();
        }
        finally {
            if ( emf != null ) {
                emf.close();
            }
        }
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        @GeneratedValue
        @Column(columnDefinition = "NUMERIC(19,0)")
        private Long id;
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class ValidateEvent {

        @Id
        @GeneratedValue
        @Column(columnDefinition = "NUMERIC(19,0)")
        private Long id;
    }
}
