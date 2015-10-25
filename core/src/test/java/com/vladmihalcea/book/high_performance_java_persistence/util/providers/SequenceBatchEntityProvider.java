package com.vladmihalcea.book.high_performance_java_persistence.util.providers;

import com.vladmihalcea.book.high_performance_java_persistence.util.EntityProvider;

import javax.persistence.*;

/**
 * <code>SequenceBatchEntityProvider</code> - SEQUENCE Batch Entity Provider
 *
 * @author Vlad Mihalcea
 */
public class SequenceBatchEntityProvider implements EntityProvider {

    @Override
    public Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Entity(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_seq")
        @SequenceGenerator(name="post_seq", sequenceName="post_seq")
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
