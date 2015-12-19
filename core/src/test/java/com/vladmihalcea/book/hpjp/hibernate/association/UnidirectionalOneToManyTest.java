package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>ManyToOneTest</code> - @ManyToOne Test
 *
 * @author Vlad Mihalcea
 */
public class UnidirectionalOneToManyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class,
            Phone.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Person person = new Person();
            Phone phone1 = new Phone("123-456-7890");
            Phone phone2 = new Phone("321-654-0987");

            person.getPhones().add(phone1);
            person.getPhones().add(phone2);
            entityManager.persist(person);
            entityManager.flush();

            person.getPhones().remove(phone1);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        @GeneratedValue
        private Long id;

        public Person() {}

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        /*@JoinTable(name = "person_phone",
            joinColumns = @JoinColumn(name = "person_id", foreignKey = @ForeignKey(name = "PERSON_ID_FK")),
            inverseJoinColumns = @JoinColumn(name = "phone_id", foreignKey = @ForeignKey(name = "PHONE_ID_FK"))
        )*/
        private List<Phone> phones = new ArrayList<>();

        public List<Phone> getPhones() {
            return phones;
        }
    }

    @Entity(name = "Phone")
    public static class Phone  {

        @Id
        @GeneratedValue
        private Long id;

        private String number;

        public Phone() {}

        public Phone(String number) {
            this.number = number;
        }

        public Long getId() {
            return id;
        }

        public String getNumber() {
            return number;
        }
    }
}
