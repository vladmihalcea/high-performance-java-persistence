package com.vladmihalcea.hpjp.spring.transaction.contract.domain;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "annex")
public class Annex implements RootAware<Contract> {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Contract contract;

    private String details;

    public Long getId() {
        return id;
    }

    public Annex setId(Long id) {
        this.id = id;
        return this;
    }

    public Contract getContract() {
        return contract;
    }

    public Annex setContract(Contract post) {
        this.contract = post;
        return this;
    }

    public String getDetails() {
        return details;
    }

    public Annex setDetails(String review) {
        this.details = review;
        return this;
    }

    @Override
    public Contract root() {
        return contract;
    }
}

