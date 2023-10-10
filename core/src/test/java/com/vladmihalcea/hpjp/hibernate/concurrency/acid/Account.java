package com.vladmihalcea.hpjp.hibernate.concurrency.acid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Account")
@Table(name = "account")
public class Account {

    @Id
    private String id;

    private String owner;

    private long balance;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getAccountBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }
}
