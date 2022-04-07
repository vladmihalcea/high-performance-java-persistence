package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "sms_subscriber")
public class SmsSubscriber extends Subscriber {

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
