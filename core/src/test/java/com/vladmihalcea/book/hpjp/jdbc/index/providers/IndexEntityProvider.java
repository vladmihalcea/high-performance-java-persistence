package com.vladmihalcea.book.hpjp.jdbc.index.providers;

import com.vladmihalcea.book.hpjp.util.EntityProvider;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

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
    public static class Task {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String status;

        public Task(String status) {
            this.status = status;
        }
    }

}
