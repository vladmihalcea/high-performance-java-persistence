package com.vladmihalcea.guide.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.assertNotNull;

/**
 * <code>DerivedIdentifierPrimaryKeyJoinColumnTest</code> - Derived Identifier Test
 *
 * @author Vlad Mihalcea
 */
public class DerivedIdentifierPrimaryKeyJoinColumnTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Person.class,
                PersonDetails.class
        };
    }

    @Test
    public void testLifecycle() {
        Long personId = doInJPA(entityManager -> {
            Person person = new Person("ABC-123");
            entityManager.persist(person);

            PersonDetails details = new PersonDetails();
            details.setPerson(person);

            entityManager.persist(details);
            return person.getId();
        });

        doInJPA(entityManager -> {
            PersonDetails details = entityManager.find(PersonDetails.class, personId);
            assertNotNull(details);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String registrationNumber;

        public Person() {}

        public Person(String registrationNumber) {
            this.registrationNumber = registrationNumber;
        }

        public Long getId() {
            return id;
        }

        public String getRegistrationNumber() {
            return registrationNumber;
        }
    }

    @Entity(name = "PersonDetails")
    public static class PersonDetails  {

        @Id
        private Long id;

        private String nickName;

        @ManyToOne
        @PrimaryKeyJoinColumn
        private Person person;

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
            this.id = person.getId();
        }
    }
}
