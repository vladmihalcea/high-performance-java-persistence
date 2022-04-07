package com.vladmihalcea.book.hpjp.hibernate.type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.PersistenceUnitInfoImpl;

/**
 * @author Vlad Mihalcea
 */
public class LongToNumericTypeTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                DummyEvent.class
        };
    }

    protected Properties validateProperties() {
        Properties properties = super.properties();
        properties.put("hibernate.hbm2ddl.auto", "validate");
        return properties;
    }

    @Test
    public void test() {
        PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl(
                LongToNumericTypeTest.class.getName(),
                Collections.singletonList( Event.class.getName() ),
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

    @Entity
    @Table(name = "event")
    public static class DummyEvent {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(columnDefinition = "NUMERIC(19,0)")
        private Long id;
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(columnDefinition = "NUMERIC(19,0)")
        private Long id;
    }
}
