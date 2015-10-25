package com.vladmihalcea.book.high_performance_java_persistence.util.providers;

import com.vladmihalcea.book.high_performance_java_persistence.util.EntityProvider;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.math.BigDecimal;

/**
 * <code>BankEntityProvider</code> - Bank Entity Provider
 *
 * @author Vlad Mihalcea
 */
public class BankEntityProvider implements EntityProvider {
    @Override
    public Class<?>[] entities() {
        return new Class<?>[]{
            Account.class
        };
    }

    @Entity(name = "account")
    public static class Account {

        @Id
        private Long id;

        private Long balance;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getBalance() {
            return balance;
        }

        public void setBalance(Long balance) {
            this.balance = balance;
        }
    }
}
