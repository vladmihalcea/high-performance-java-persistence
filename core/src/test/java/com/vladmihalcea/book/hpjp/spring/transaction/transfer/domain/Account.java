package com.vladmihalcea.book.hpjp.spring.transaction.transfer.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Account")
@Table(name = "account")
public class Account {

    @Id
    private String iban;

    private String owner;

    private long balance;

    @Version
    private short version;

    public String getIban() {
        return iban;
    }

    public Account setIban(String iban) {
        this.iban = iban;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public Account setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public long getBalance() {
        return balance;
    }

    public Account setBalance(long balance) {
        this.balance = balance;
        return this;
    }
}
