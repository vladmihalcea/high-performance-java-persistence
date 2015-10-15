package com.vladmihalcea.book.high_performance_java_persistence.jdbc.index.providers;

import com.vladmihalcea.book.high_performance_java_persistence.util.EntityProvider;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <code>BatchEntityProvider</code> - Batch Entity Provider
 *
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
