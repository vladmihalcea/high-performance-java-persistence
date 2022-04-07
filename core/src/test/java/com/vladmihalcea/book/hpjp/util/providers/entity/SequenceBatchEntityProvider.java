package com.vladmihalcea.book.hpjp.util.providers.entity;

import com.vladmihalcea.book.hpjp.util.EntityProvider;

import jakarta.persistence.*;

/**
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
        @SequenceGenerator(name="post_seq", sequenceName="post_seq", allocationSize = 1)
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
