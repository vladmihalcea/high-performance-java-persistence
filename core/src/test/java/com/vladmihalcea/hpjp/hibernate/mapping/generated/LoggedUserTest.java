package com.vladmihalcea.hpjp.hibernate.mapping.generated;

import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.util.EnumSet;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * @author Vlad Mihalcea
 */
public class LoggedUserTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Sensor.class
        };
    }

    @Override
    protected void afterInit() {
        LoggedUser.logIn("Alice");

        doInJPA(entityManager -> {
            Sensor ip = new Sensor();
            ip.setName("ip");
            ip.setValue("192.168.0.101");

            entityManager.persist(ip);

            executeSync(() -> {
                LoggedUser.logIn("Bob");

                doInJPA(_entityManager -> {
                    Sensor temperature = new Sensor();
                    temperature.setName("temperature");
                    temperature.setValue("32");

                    _entityManager.persist(temperature);
                });

                LoggedUser.logOut();
            });
        });

        LoggedUser.logOut();
    }

    @Test
    public void test() {
        LoggedUser.logIn("Alice");

        doInJPA(entityManager -> {
            Sensor temperature = entityManager.find(Sensor.class, "temperature");

            temperature.setValue("36");

            executeSync(() -> {
                LoggedUser.logIn("Bob");

                doInJPA(_entityManager -> {
                    Sensor ip = _entityManager.find(Sensor.class, "ip");

                    ip.setValue("192.168.0.102");
                });

                LoggedUser.logOut();
            });
        });

        LoggedUser.logOut();
    }

    @Entity(name = "Sensor")
    @Table(name = "sensor")
    public static class Sensor {

        @Id
        @Column(name = "sensor_name")
        private String name;

        @Column(name = "sensor_value")
        private String value;

        @Column(name = "created_by")
        @CurrentLoggedUser
        private String createdBy;

        @Column(name = "updated_by")
        @CurrentLoggedUser
        private String updatedBy;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }
    }

    public static class LoggedUser {

        private static final ThreadLocal<String> userHolder = new ThreadLocal<>();

        public static void logIn(String user) {
            userHolder.set(user);
        }

        public static void logOut() {
            userHolder.remove();
        }

        public static String get() {
            return userHolder.get();
        }
    }

    public static class LoggedUserGenerator implements AnnotationBasedGenerator, BeforeExecutionGenerator {

        public LoggedUserGenerator() {
        }

        @Override
        public void initialize(Annotation annotation, Member member, GeneratorCreationContext generatorCreationContext) {

        }

        @Override
        public Object generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o, Object o1, EventType eventType) {
            return LoggedUser.get();
        }

        @Override
        public EnumSet<EventType> getEventTypes() {
            return EventTypeSets.INSERT_AND_UPDATE;
        }
    }

    @ValueGenerationType(generatedBy = LoggedUserGenerator.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({FIELD, METHOD})
    public @interface CurrentLoggedUser { }
}
