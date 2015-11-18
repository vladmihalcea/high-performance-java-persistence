package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class JtaEntity {

    @Id
    private Long id;

    public JtaEntity() {
    }

    public JtaEntity(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
