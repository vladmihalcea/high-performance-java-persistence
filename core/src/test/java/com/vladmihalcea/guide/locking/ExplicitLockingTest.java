package com.vladmihalcea.guide.locking;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <code>ExplicitLockingTest</code> - Explicit Locking Test
 *
 * @author Vlad Mihalcea
 */
public class ExplicitLockingTest extends AbstractMySQLIntegrationTest {

    private static final Logger log = Logger.getLogger(ExplicitLockingTest.class);

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Person.class,
        };
    }

    @Test
    public void testBuildLockRequest() {
        doInJPA(entityManager -> {
            Person person = new Person("John Doe");
            entityManager.persist(person);
        });
        Person personEntity = doInJPA(entityManager -> {
            log.info("testBuildLockRequest");
            Long id = 1L;
            Person person = entityManager.find(Person.class, id);
            Session session = entityManager.unwrap(Session.class);
            session
                .buildLockRequest(LockOptions.NONE)
                .setLockMode(LockMode.PESSIMISTIC_READ)
                .setTimeOut(LockOptions.NO_WAIT)
                .lock(person);
            return person;
        });
    }

    @Test
    public void testJPALockTimeout() {
        doInJPA(entityManager -> {
            Person person = new Person("John Doe");
            entityManager.persist(person);
        });
        doInJPA(entityManager -> {
            log.info("testJPALockTimeout");
            Long id = 1L;
            entityManager.find(Person.class, id, LockModeType.PESSIMISTIC_WRITE,
                Collections.singletonMap("javax.persistence.lock.timeout", 2000));
        });
    }

    @Test
    public void testJPALockScope() {
        doInJPA(entityManager -> {
            Person person = new Person("John Doe");
            entityManager.persist(person);
            Phone home = new Phone( "123-456-7890" );
            Phone office = new Phone( "098-765-4321" );
            person.getPhones().add( home );
            person.getPhones().add( office );
            entityManager.persist(person);
        });
        doInJPA(entityManager -> {
            log.info("testJPALockScope NORMAL");
            Long id = 1L;
            Person person = entityManager.find(Person.class, id, LockModeType.PESSIMISTIC_WRITE,
                    Collections.singletonMap("javax.persistence.lock.scope", PessimisticLockScope.NORMAL));
        });
        doInJPA(entityManager -> {
            log.info("testJPALockScope EXTENDED");
            Long id = 1L;
            Person person = entityManager.find(Person.class, id, LockModeType.PESSIMISTIC_WRITE,
                    Collections.singletonMap("javax.persistence.lock.scope", PessimisticLockScope.EXTENDED));
        });
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "`name`")
        private String name;

        @ElementCollection
        @JoinTable(name = "person_phone", joinColumns = @JoinColumn(name = "person_id"))
        private List<Phone> phones = new ArrayList<>();

        public Person() {
        }

        public Person(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<Phone> getPhones() {
            return phones;
        }
    }

    @Embeddable
    public static class Phone {

        @Column
        private String mobile;

        public Phone() {}

        public Phone(String mobile) {
            this.mobile = mobile;
        }
    }
}
