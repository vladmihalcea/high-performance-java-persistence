package com.vladmihalcea.book.hpjp.util.providers.entity;

import com.vladmihalcea.book.hpjp.util.EntityProvider;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
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
