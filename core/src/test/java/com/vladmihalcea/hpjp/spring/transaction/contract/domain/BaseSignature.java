package com.vladmihalcea.hpjp.spring.transaction.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDate;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
public class BaseSignature<T extends BaseSignature<T>> {

    @Id
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "signed_on")
    private LocalDate signedOn = LocalDate.now();

    public Long getId() {
        return id;
    }

    public T setId(Long id) {
        this.id = id;
        return (T) this;
    }

    public String getFirstName() {
        return firstName;
    }

    public T setFirstName(String firstName) {
        this.firstName = firstName;
        return (T) this;
    }

    public String getLastName() {
        return lastName;
    }

    public T setLastName(String lastName) {
        this.lastName = lastName;
        return (T) this;
    }

    public LocalDate getSignedOn() {
        return signedOn;
    }

    public T setSignedOn(LocalDate signedOn) {
        this.signedOn = signedOn;
        return (T) this;
    }
}
