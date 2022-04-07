package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class NativeQueryEntityMappingTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            Tag.class,
        };
    }

    @Test
    public void test() {
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
            List<Object[]> tuples = entityManager.createNativeQuery(
                "SELECT * "+
                "FROM post p "+
                "LEFT JOIN post_tag pt ON p.id = pt.post_id "+
                "LEFT JOIN tag t ON t.id = pt.tag_id ")
            .unwrap( NativeQuery.class )
            .addEntity("post", Post.class)
            .addJoin("tag", "post.tags")
            .getResultList();

            assertEquals(3, tuples.size());
        });

        doInJPA(entityManager -> {
            List<Post> tuples = entityManager
            .createNamedQuery("find_posts_with_tags")
            .getResultList();

            assertEquals(3, tuples.size());
        });
    }

    @NamedNativeQuery(
            name = "find_posts_with_tags",
            query =
                "SELECT " +
                "       p.id as \"p.id\", "+
                "       p.title as \"p.title\", "+
                "       t.id as \"t.id\", "+
                "       t.name as \"t.name\" "+
                "FROM post p "+
                "LEFT JOIN post_tag pt ON p.id = pt.post_id "+
                "LEFT JOIN tag t ON t.id = pt.tag_id ",
            resultSetMapping = "posts_with_tags"
    )
    @SqlResultSetMapping(
            name = "posts_with_tags",
            entities = {
                    @EntityResult(
                            entityClass = Post.class,
                            fields = {
                                @FieldResult( name = "id", column = "p.id" ),
                                @FieldResult( name = "title", column = "p.title" ),
                            }
                    ),
                    @EntityResult(
                            entityClass = Tag.class,
                            fields = {
                                @FieldResult( name = "id", column = "t.id" ),
                                @FieldResult( name = "name", column = "t.name" ),
                            }
                    )
            }
    )
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
        private Set<Tag> tags = new HashSet<>();

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

        public Set<Tag> getTags() {
            return tags;
        }

        public void setTags(Set<Tag> tags) {
            this.tags = tags;
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
            if (o == null || getClass() != o.getClass()) return false;
            Post post = (Post) o;
            return Objects.equals( title, post.title);
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
        @GeneratedValue
        private Long id;

        private String name;

        @ManyToMany(mappedBy = "tags")
        private Set<Post> posts = new HashSet<>();

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

        public Set<Post> getPosts() {
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
