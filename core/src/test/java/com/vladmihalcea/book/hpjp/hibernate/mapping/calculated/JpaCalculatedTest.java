package com.vladmihalcea.book.hpjp.hibernate.mapping.calculated;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JpaCalculatedTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Account.class,
            User.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            User user = new User();
            user.setId(1L);
            user.setFirstName("John");
            user.setFirstName("Doe");

            entityManager.persist(user);

            Account account = new Account(
                1L,
                user,
                "ABC123",
                12345L,
                6.7,
                Timestamp.valueOf(
                        LocalDateTime.now().minusMonths(3)
                )
            );

            entityManager.persist(account);
        });
        doInJPA(entityManager -> {
            Account account = entityManager.find(Account.class, 1L);

            assertEquals(123.45D, account.getDollars(), 0.001);
            assertEquals(207L, account.getInterestCents());
            assertEquals(2.07D, account.getInterestDollars(), 0.001);
        });
    }

    @Entity(name = "Account")
    @Table(name = "account")
    public static class Account {

        @Id
        private Long id;

        @ManyToOne
        private User owner;

        private String iban;

        private long cents;

        private double interestRate;

        private Timestamp createdOn;

        public Account() {
        }

        public Account(Long id, User owner, String iban, long cents, double interestRate, Timestamp createdOn) {
            this.id = id;
            this.owner = owner;
            this.iban = iban;
            this.cents = cents;
            this.interestRate = interestRate;
            this.createdOn = createdOn;
        }

        @Transient
        public double getDollars() {
            return cents / 100D;
        }

        @Transient
        public long getInterestCents() {
            long months = createdOn.toLocalDateTime().until(LocalDateTime.now(), ChronoUnit.MONTHS);
            double interestUnrounded = ( ( interestRate / 100D ) * cents * months ) / 12;
            return BigDecimal.valueOf(interestUnrounded).setScale(0, BigDecimal.ROUND_HALF_EVEN).longValue();
        }

        @Transient
        public double getInterestDollars() {
            return getInterestCents() / 100D;
        }
    }

    @Entity(name = "User")
    @Table(name = "`user`")
    public static class User {

        @Id
        private Long id;

        private String firstName;

        private String lastName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
