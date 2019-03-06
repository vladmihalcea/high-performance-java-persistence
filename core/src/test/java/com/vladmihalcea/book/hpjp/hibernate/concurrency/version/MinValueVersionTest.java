package com.vladmihalcea.book.hpjp.hibernate.concurrency.version;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.ShortType;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MinValueVersionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Test
    public void testOptimisticLocking() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            assertEquals(Short.MIN_VALUE, post.getVersion());

            entityManager.flush();
            post.setTitle("High-Performance Hibernate");
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(Short.MIN_VALUE + 1, post.getVersion());
        });
    }

    @Entity(name = "Post")  @Table(name = "post")
    @TypeDef(name = "short-version", typeClass = ShortVersionType.class)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        @Type(type = "short-version")
        private Short version;

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

        public int getVersion() {
            return version;
        }
    }

}
