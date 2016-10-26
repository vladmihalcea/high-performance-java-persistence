package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class HibernateSaveSequenceTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }


    @Test
    public void testId() {

        doInHibernate(session -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");

            Long identifier = (Long) session.save(post);
            LOGGER.info("The post entity identifier is {}", identifier);

            LOGGER.info("Flush Persistence Context");
            session.flush();
        });
    }

    @Entity(name = "Post") @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Version
        private Long version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
