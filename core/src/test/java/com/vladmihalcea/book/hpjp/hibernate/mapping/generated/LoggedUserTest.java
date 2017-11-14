package com.vladmihalcea.book.hpjp.hibernate.mapping.generated;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.tuple.ValueGenerator;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
public class LoggedUserTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Sensor.class
        };
    }

    @Override
    protected void afterInit() {
        LoggedUser.INSTANCE.logIn("Alice");

        doInJPA(entityManager -> {
            Sensor ip = new Sensor();
            ip.setName("ip");
            ip.setValue("192.168.0.101");

            entityManager.persist(ip);

            executeSync(() -> {
                LoggedUser.INSTANCE.logIn("Bob");

                doInJPA(_entityManager -> {
                    Sensor temperature = new Sensor();
                    temperature.setName("temperature");
                    temperature.setValue("32");

                    _entityManager.persist(temperature);
                });

                LoggedUser.INSTANCE.logOut();
            });
        });

        LoggedUser.INSTANCE.logOut();
    }

    @Test
    public void test() {
        LoggedUser.INSTANCE.logIn("Alice");

        doInJPA(entityManager -> {
            Sensor temperature = entityManager.find(Sensor.class, "temperature");

            temperature.setValue("36");

            executeSync(() -> {
                LoggedUser.INSTANCE.logIn("Bob");

                doInJPA(_entityManager -> {
                    Sensor ip = _entityManager.find(Sensor.class, "ip");

                    ip.setValue("192.168.0.102");
                });

                LoggedUser.INSTANCE.logOut();
            });
        });

        LoggedUser.INSTANCE.logOut();
    }

    @Entity(name = "Sensor")
    public static class Sensor {

        @Id
        @Column(name = "sensor_name")
        private String name;

        @Column(name = "sensor_value")
        private String value;

        @Column(name = "created_by")
        @GeneratorType(
                type = LoggedUserGenerator.class,
                when = GenerationTime.INSERT
        )
        private String createdBy;

        @Column(name = "updated_by")
        @GeneratorType(
                type = LoggedUserGenerator.class,
                when = GenerationTime.ALWAYS
        )
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

        public static final LoggedUser INSTANCE = new LoggedUser();

        private static final ThreadLocal<String> userHolder = new ThreadLocal<>();

        public void logIn(String user) {
            userHolder.set(user);
        }

        public void logOut() {
            userHolder.remove();
        }

        public String get() {
            return userHolder.get();
        }
    }

    public static class LoggedUserGenerator
            implements ValueGenerator<String> {

        @Override
        public String generateValue(
                Session session, Object owner) {
            return LoggedUser.INSTANCE.get();
        }
    }
}
