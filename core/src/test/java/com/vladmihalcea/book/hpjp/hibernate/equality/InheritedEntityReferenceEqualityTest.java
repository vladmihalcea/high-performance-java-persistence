package com.vladmihalcea.book.hpjp.hibernate.equality;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Hibernate;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.util.Objects;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class InheritedEntityReferenceEqualityTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Topic.class,
            Post.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testInheritedEntityReferenceEqualityInOneTransaction() {
        doInJPA(entityManager -> {
            entityManager.persist(new Post(1L, "Title", "Content"));

            Topic postProxy = entityManager.getReference(Topic.class, 1L);
            Topic post = (Topic) Hibernate.unproxy(postProxy);

            assertTrue("The entity is not equal with the entity proxy.", post.equals(postProxy));
        });
    }

    @Test
    public void testInheritedEntityReferenceEqualityInDifferentTransactions() {
        doInJPA(entityManager -> {
            entityManager.persist(new Post(1L, "Title", "Content"));
        });
        doInJPA(entityManager -> {
            Topic postProxy = entityManager.getReference(Topic.class, 1L);
            Topic post = (Topic) Hibernate.unproxy(postProxy);

            assertTrue("The entity is not equal with the entity proxy.", post.equals(postProxy));
        });
    }

    @Entity(name = "Topic")
    @Table(name = "topics")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    @DiscriminatorColumn(name = "type")
    public abstract static class Topic {

        @Id
        private Long id;

        @Column
        private String title;

        public Topic() {}

        public Topic(Long id, String title) {
            this.id = id;
            this.title = title;
        }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Topic)) return false;
            Topic topic = (Topic) o;
            return Objects.equals(id, topic.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @Entity(name = "Post")
    @DiscriminatorValue("POST")
    public static class Post extends Topic {

        @Column
        private String content;

        public Post() {}

        public Post(Long id, String title, String content) {
            super(id, title);
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
