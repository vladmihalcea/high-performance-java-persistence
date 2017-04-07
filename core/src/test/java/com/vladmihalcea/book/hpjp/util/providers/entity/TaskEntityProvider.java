package com.vladmihalcea.book.hpjp.util.providers.entity;

import com.vladmihalcea.book.hpjp.util.EntityProvider;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
public class TaskEntityProvider implements EntityProvider {

    public enum StatusType {
        TO_D0,
        DONE,
        FAILED
    }

    @Override
    public Class<?>[] entities() {
        return new Class<?>[]{
                Task.class
        };
    }

    @Entity(name = "task")
    public static class Task {

        @Id
        private Long id;

        @Enumerated(EnumType.STRING)
        private StatusType status;

        @Embedded
        private Change change;
    }

    @Embeddable
    public static class Change {

        @Column(name = "changed_on")
        private Date changedOn;

        @Column(name = "created_by")
        private String changedBy;
    }
}
