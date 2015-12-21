package com.vladmihalcea.guide.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

/**
 * <code>OneToOneTest</code> - @OneToOne Test
 *
 * @author Vlad Mihalcea
 */
public class OneToOneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Phone.class,
            PhoneDetails.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Phone phone = new Phone("123-456-7890");
            PhoneDetails details = new PhoneDetails("T-Mobile", "GSM");

            phone.setDetails(details);
            entityManager.persist(phone);
            entityManager.persist(details);
        });
    }

    @Entity(name = "Phone")
    public static class Phone  {

        @Id
        @GeneratedValue
        private Long id;

        private String number;

        @OneToOne
        @JoinColumn(name = "details_id")
        private PhoneDetails details;

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

        public PhoneDetails getDetails() {
            return details;
        }

        public void setDetails(PhoneDetails details) {
            this.details = details;
        }
    }

    @Entity(name = "PhoneDetails")
    public static class PhoneDetails  {

        @Id
        @GeneratedValue
        private Long id;

        private String provider;

        private String technology;

        public PhoneDetails() {}

        public PhoneDetails(String provider, String technology) {
            this.provider = provider;
            this.technology = technology;
        }

        public String getProvider() {
            return provider;
        }

        public String getTechnology() {
            return technology;
        }

        public void setTechnology(String technology) {
            this.technology = technology;
        }
    }
}
