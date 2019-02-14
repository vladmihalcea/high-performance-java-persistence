package com.vladmihalcea.book.hpjp.hibernate.cache.query;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table(name = "test")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class TestEntity {
    @Id
    @GeneratedValue(generator = "test_seq")
    @SequenceGenerator(name = "test_seq", sequenceName="TEST_SEQ")
    @Column(name = "id")
    private int id;

    @Column(name = "value", nullable = false)
    private String value;

    public TestEntity() {
    }

    public TestEntity(String value) {
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}