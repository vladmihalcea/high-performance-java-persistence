package com.vladmihalcea.hpjp.jdbc.index.providers;

import com.vladmihalcea.hpjp.util.EntityProvider;

import jakarta.persistence.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Vlad Mihalcea
 */
public class IndexEntityProvider implements EntityProvider {

    @Override
    public Class<?>[] entities() {
        return new Class<?>[]{
            Task.class
        };
    }

    @Entity(name = "Task")
    @Table(name = "task")
    public static class Task {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @Enumerated(EnumType.STRING)
        private Status status;

        public Long getId() {
            return id;
        }

        public Task setId(Long id) {
            this.id = id;
            return this;
        }

        public Status getStatus() {
            return status;
        }

        public Task setStatus(Status status) {
            this.status = status;
            return this;
        }

        public enum Status {
            DONE,
            TO_DO,
            FAILED;

            public static Status random() {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                Status[] values = Status.values();
                return values[random.nextInt(values.length)];
            }
        }
    }
}
