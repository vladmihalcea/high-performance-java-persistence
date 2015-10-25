package com.vladmihalcea.book.high_performance_java_persistence.util.providers;

import com.vladmihalcea.book.high_performance_java_persistence.util.EntityProvider;

import javax.persistence.*;

/**
 * <code>BatchEntityProvider</code> - Batch Entity Provider
 *
 * @author Vlad Mihalcea
 */
public class AutoIncrementBatchEntityProvider implements EntityProvider {

    @Override
    public Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Entity(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String title;

        @Version
        private int version;

        private Post() {
        }

        public Post(String title) {
            this.title = title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
