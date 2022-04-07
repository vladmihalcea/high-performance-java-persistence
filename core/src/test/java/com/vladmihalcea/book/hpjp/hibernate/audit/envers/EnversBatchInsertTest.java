package com.vladmihalcea.book.hpjp.hibernate.audit.envers;

import com.vladmihalcea.book.hpjp.hibernate.association.BidirectionalManyToManyListTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.NaturalId;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EnversBatchInsertTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class,
            Tag.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(
            EnversSettings.AUDIT_STRATEGY,
            ValidityAuditStrategy.class.getName()
        );
        properties.put("hibernate.jdbc.batch_size", "5");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    /**
     * See issue https://hibernate.atlassian.net/browse/HHH-14664}
     */
    @Test
    public void test_HHH_14664() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 10; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle(
                    String.format(
                        "High-Performance Java Persistence edition - %d", i
                    )
                );
                Tag tag = new Tag();
                tag.setName(String.format("Tag %d", i));
                post.addTag(tag);
                entityManager.persist(post);
            }
            entityManager.flush();
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance Java Persistence 2nd edition");
        });

        doInJPA(entityManager -> {
            List<Post> posts = AuditReaderFactory.get(entityManager)
                .createQuery()
                .forRevisionsOfEntity(Post.class, true, true)
                .add(AuditEntity.id().eq(1L))
                .getResultList();

            assertEquals(2, posts.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @Audited
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        @Audited
        private List<Tag> tags = new ArrayList<>();

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
        public String toString() {
            return "Post{" +
                   "id=" + id +
                   ", title='" + title + '\'' +
                   '}';
        }

        public void addTag(Tag tag) {
            tags.add(tag);
            tag.getPosts().add(this);
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    @Audited
    public static class Tag {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String name;

        @ManyToMany(mappedBy = "tags")
        private List<Post> posts = new ArrayList<>();

        public Tag() {}

        public Tag(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Post> getPosts() {
            return posts;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tag tag = (Tag) o;
            return Objects.equals(name, tag.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
