package com.vladmihalcea.guide.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.DiscriminatorFormula;
import org.junit.Test;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * <code>SingleTableTest</code> - Single Table Test
 *
 * @author Vlad Mihalcea
 */
public class SingleTableDiscriminatorFormulaTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                DebitAccount.class,
                CreditAccount.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            DebitAccount debitAccount = new DebitAccount("123-debit");
            debitAccount.setId(1L);
            debitAccount.setOwner("John Doe");
            debitAccount.setBalance(BigDecimal.valueOf(100));
            debitAccount.setInterestRate(BigDecimal.valueOf(1.5d));
            debitAccount.setOverdraftFee(BigDecimal.valueOf(25));

            CreditAccount creditAccount = new CreditAccount("456-credit");
            creditAccount.setId(2L);
            creditAccount.setOwner("John Doe");
            creditAccount.setBalance(BigDecimal.valueOf(1000));
            creditAccount.setInterestRate(BigDecimal.valueOf(1.9d));
            creditAccount.setCreditLimit(BigDecimal.valueOf(5000));

            entityManager.persist(debitAccount);
            entityManager.persist(creditAccount);
        });

        doInJPA(entityManager -> {
            List<Account> accounts =
                entityManager.createQuery("select a from Account a").getResultList();
            assertEquals(2, accounts.size());
        });
    }

    @Entity(name = "Account")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    @DiscriminatorFormula(
        "case when debitKey is not null " +
        "then 'Debit' " +
        "else ( " +
        "   case when creditKey is not null " +
        "   then 'Credit' " +
        "   else 'Unknown' " +
        "   end ) " +
        "end "
    )
    public static class Account {

        @Id
        private Long id;

        private String owner;

        private BigDecimal balance;

        private BigDecimal interestRate;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public BigDecimal getInterestRate() {
            return interestRate;
        }

        public void setInterestRate(BigDecimal interestRate) {
            this.interestRate = interestRate;
        }
    }

    @Entity(name = "DebitAccount")
    @DiscriminatorValue(value = "Debit")
    public static class DebitAccount extends Account {

        private String debitKey;

        private BigDecimal overdraftFee;

        private DebitAccount() {}

        public DebitAccount(String debitKey) {
            this.debitKey = debitKey;
        }

        public String getDebitKey() {
            return debitKey;
        }

        public BigDecimal getOverdraftFee() {
            return overdraftFee;
        }

        public void setOverdraftFee(BigDecimal overdraftFee) {
            this.overdraftFee = overdraftFee;
        }
    }

    @Entity(name = "CreditAccount")
    @DiscriminatorValue(value = "Credit")
    public static class CreditAccount extends Account {

        private String creditKey;

        private BigDecimal creditLimit;

        private CreditAccount() {}

        public CreditAccount(String creditKey) {
            this.creditKey = creditKey;
        }

        public String getCreditKey() {
            return creditKey;
        }

        public BigDecimal getCreditLimit() {
            return creditLimit;
        }

        public void setCreditLimit(BigDecimal creditLimit) {
            this.creditLimit = creditLimit;
        }
    }
}
