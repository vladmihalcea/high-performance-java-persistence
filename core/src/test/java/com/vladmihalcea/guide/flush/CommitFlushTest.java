package com.vladmihalcea.guide.flush;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <code>CommitFlushTest</code> - Commit Flush Test
 *
 * @author Vlad Mihalcea
 */
public class CommitFlushTest extends AbstractTest {

    private static final Logger log = Logger.getLogger(CommitFlushTest.class);

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Person.class,
                Phone.class,
                Advertisement.class,
        };
    }

    @Test
    public void testFlushJPQL() {
        doInJPA(entityManager -> {
            log.info("testFlushJPQL");
            Person person = new Person("John Doe");
            entityManager.persist(person);
            entityManager.createQuery("select p from Advertisement p")
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
            entityManager.createQuery("select p from Person p")
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
        });
    }

    @Test
    public void testFlushSQL() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("delete from Person")
                    .executeUpdate();
        });
        doInJPA(entityManager -> {
            log.info("testFlushSQL");
            Person person = new Person("John Doe");
            entityManager.persist(person);
            assertTrue(((Number) entityManager
                .createNativeQuery("select count(*) from Person")
                .getSingleResult()).intValue() > 0);
        });
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
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

        public void addPhone(Phone phone) {
            phones.add(phone);
            phone.setPerson(this);
        }
    }

    @Entity(name = "Phone")
    public static class Phone {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne
        private Person person;

        private String number;

        public Phone() {
        }

        public Phone(String number) {
            this.number = number;
        }

        public Long getId() {
            return id;
        }

        public String getNumber() {
            return number;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }
    }

    @Entity(name = "Advertisement")
    public static class Advertisement {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
