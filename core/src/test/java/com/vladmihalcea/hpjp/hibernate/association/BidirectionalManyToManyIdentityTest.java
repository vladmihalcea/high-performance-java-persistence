package com.vladmihalcea.hpjp.hibernate.association;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalManyToManyIdentityTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Tag.class
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "100");
        properties.put(AvailableSettings.ORDER_INSERTS, "100");
    }

    @Test
    public void testBatchInserts() {
        Post post1 = new Post("JPA with Hibernate");
        Post post2 = new Post("Native Hibernate");

        Tag tag1 = new Tag("Java");
        Tag tag2 = new Tag("Hibernate");

        doInJPA(entityManager -> {
            entityManager.persist(post1);
            entityManager.persist(post2);
            entityManager.persist(tag1);
            entityManager.persist(tag2);
        });

        doInJPA(entityManager -> {
            Post _post1 = entityManager.find(Post.class, post1.getId());
            Post _post2 = entityManager.find(Post.class, post2.getId());

            Tag _tag1 = entityManager.find(Tag.class, tag1.getId());
            Tag _tag2 = entityManager.find(Tag.class, tag2.getId());
            _post1.addTag(_tag1);
            _post1.addTag(_tag2);

            _post2.addTag(_tag1);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String title;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

        @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE})
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
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

        public List<Tag> getTags() {
            return tags;
        }

        public void addTag(Tag tag) {
            tags.add(tag);
            tag.getPosts().add(this);
        }

        public void removeTag(Tag tag) {
            tags.remove(tag);
            tag.getPosts().remove(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof Post))
                return false;

            Post post = (Post) o;
            return Objects.equals(title, post.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title);
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

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
            if (this == o)
                return true;

            if (!(o instanceof Tag))
                return false;

            Tag tag = (Tag) o;
            return Objects.equals(name, tag.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
