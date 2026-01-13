package com.vladmihalcea.hpjp.hibernate.identifier.override.identity2sequence;

import io.hypersistence.utils.hibernate.id.BatchSequence;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
public class AbstractEntity {

    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    //@GeneratedValue(strategy = GenerationType.SEQUENCE)
    @BatchSequence(
        name = "post_id_seq",
        fetchSize = 5
    )
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
