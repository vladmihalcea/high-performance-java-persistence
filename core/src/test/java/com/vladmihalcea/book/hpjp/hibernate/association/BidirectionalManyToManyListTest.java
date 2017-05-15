package com.vladmihalcea.book.hpjp.hibernate.association;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalManyToManyListTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Tag.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post1 = new Post("JPA with Hibernate");
            Post post2 = new Post("Native Hibernate");

            Tag tag1 = new Tag("Java");
            Tag tag2 = new Tag("Hibernate");

            post1.addTag(tag1);
            post1.addTag(tag2);

            post2.addTag(tag1);

            entityManager.persist(post1);
            entityManager.persist(post2);

            entityManager.flush();

            post1.removeTag(tag1);
        });
    }

    @Test
    public void testRemove() {
        final Long postId = doInJPA(entityManager -> {
            Post post1 = new Post("JPA with Hibernate");
            Post post2 = new Post("Native Hibernate");

            Tag tag1 = new Tag("Java");
            Tag tag2 = new Tag("Hibernate");

            post1.addTag(tag1);
            post1.addTag(tag2);

            post2.addTag(tag1);

            entityManager.persist(post1);
            entityManager.persist(post2);

            return post1.id;
        });
        doInJPA(entityManager -> {
            LOGGER.info("Remove");
            Post post1 = entityManager.find(Post.class, postId);

            entityManager.remove(post1);
        });
    }

    @Test
    public void testShuffle() {
        final Long postId = doInJPA(entityManager -> {
            Post post1 = new Post("JPA with Hibernate");
            Post post2 = new Post("Native Hibernate");

            Tag tag1 = new Tag("Java");
            Tag tag2 = new Tag("Hibernate");

            post1.addTag(tag1);
            post1.addTag(tag2);

            post2.addTag(tag1);

            entityManager.persist(post1);
            entityManager.persist(post2);

            return post1.id;
        });
        doInJPA(entityManager -> {
            LOGGER.info("Shuffle");
            Tag tag1 = new Tag("Java");
            Post post1 = entityManager
            .createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.tags " +
                "where p.id = :id", Post.class)
            .setParameter( "id", postId )
            .getSingleResult();

            post1.removeTag(tag1);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
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
            if (this == o) return true;
            if (!(o instanceof Post)) return false;
            return id != null && id.equals(((Post) o).id);
        }

        @Override
        public int hashCode() {
            return 31;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
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
