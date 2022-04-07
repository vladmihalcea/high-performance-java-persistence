package com.vladmihalcea.book.hpjp.hibernate.mapping.calculated;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JPACalculatedPostLoadTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
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

        @Transient
        private double dollars;

        @Transient
        private long interestCents;

        @Transient
        private double interestDollars;

        public Account() {
        }

        public Account(
                Long id, User owner, String iban,
                long cents, double interestRate, Timestamp createdOn) {
            this.id = id;
            this.owner = owner;
            this.iban = iban;
            this.cents = cents;
            this.interestRate = interestRate;
            this.createdOn = createdOn;
        }

        @PostLoad
        private void postLoad() {
            this.dollars = cents / 100D;

            long months = createdOn.toLocalDateTime().until(
                LocalDateTime.now(),
                ChronoUnit.MONTHS)
            ;

            double interestUnrounded = (
                (interestRate / 100D) * cents * months
            ) / 12;

            this.interestCents = BigDecimal.valueOf(interestUnrounded)
                .setScale(0, BigDecimal.ROUND_HALF_EVEN)
                .longValue();

            this.interestDollars = interestCents / 100D;
        }

        public double getDollars() {
            return dollars;
        }

        public long getInterestCents() {
            return interestCents;
        }

        public double getInterestDollars() {
            return interestDollars;
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
