package com.vladmihalcea.book.hpjp.hibernate.mapping.calculated;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.Formula;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class FormulaTest extends AbstractPostgreSQLIntegrationTest {

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

        @ManyToOne(fetch = FetchType.LAZY)
        private User owner;

        private String iban;

        private long cents;

        private double interestRate;

        private Timestamp createdOn;

        @Formula("cents::numeric / 100")
        private double dollars;

        @Formula(
            "round(" +
            "   (interestRate::numeric / 100) * " +
            "   cents * " +
            "   date_part('month', age(now(), createdOn)" +
            ") " +
            "/ 12)")
        private long interestCents;

        @Formula(
            "round(" +
            "   (interestRate::numeric / 100) * " +
            "   cents * " +
            "   date_part('month', age(now(), createdOn)" +
            ") " +
            "/ 12) " +
            "/ 100::numeric")
        private double interestDollars;

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
            return dollars;
        }

        @Transient
        public long getInterestCents() {
            return interestCents;
        }

        @Transient
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
