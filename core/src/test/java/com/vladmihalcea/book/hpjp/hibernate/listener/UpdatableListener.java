package com.vladmihalcea.book.hpjp.hibernate.listener;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
public class UpdatableListener {

    @PrePersist
    @PreUpdate
    private void setCurrentTimestamp(Object entity) {
        if(entity instanceof Updatable) {
            Updatable updatable = (Updatable) entity;
            updatable.setTimestamp(new Date());
        }
    }

}
