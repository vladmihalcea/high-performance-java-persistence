package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "subscriber")
@Inheritance(strategy = InheritanceType.JOINED)
public class Subscriber {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    @Column(name = "created_on")
    private Date createdOn;

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

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }
}
