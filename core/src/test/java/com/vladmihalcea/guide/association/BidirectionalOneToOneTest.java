package com.vladmihalcea.guide.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.fail;

/**
 * <code>BidirectionalOneToOneTest</code> - Bidirectional @OneToOne Test
 *
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToOneTest extends AbstractTest {

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

            phone.addDetails(details);
            entityManager.persist(phone);
        });
    }

    @Test
    public void testConstraint() {
        try {
            doInJPA(entityManager -> {
                Phone phone = new Phone("123-456-7890");
                PhoneDetails details = new PhoneDetails("T-Mobile", "GSM");

                phone.addDetails(details);
                entityManager.persist(phone);

                PhoneDetails otherDetails = new PhoneDetails("T-Mobile", "CDMA");
                otherDetails.setPhone(phone);
                entityManager.persist(otherDetails);
                entityManager.flush();
                entityManager.clear();

                phone = entityManager.find(Phone.class, phone.getId());
                phone.getDetails().getProvider();
            });
            fail("Expected: HHH000327: Error performing load command : org.hibernate.HibernateException: More than one row with the given identifier was found: 1");
        } catch (Exception expected) {
            LOGGER.error("Expected", expected);
        }
    }

    @Entity(name = "Phone")
    public static class Phone  {

        @Id
        @GeneratedValue
        private Long id;

        private String number;

        @OneToOne(mappedBy = "phone", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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

        public void addDetails(PhoneDetails details) {
            details.setPhone(this);
            this.details = details;
        }

        public void removeDetails() {
            if (details != null) {
                details.setPhone(null);
                this.details = null;
            }
        }
    }

    @Entity(name = "PhoneDetails")
    public static class PhoneDetails  {

        @Id
        @GeneratedValue
        private Long id;

        private String provider;

        private String technology;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "phone_id")
        private Phone phone;

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

        public Phone getPhone() {
            return phone;
        }

        public void setPhone(Phone phone) {
            this.phone = phone;
        }
    }
}
