package com.vladmihalcea.book.hpjp.hibernate.association;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.junit.Test;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ElementCollectionSetMergeTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostCategory.class
        };
    }

    @Override
    protected Database database() {
        return Database.HSQLDB;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            PostCategory category1 = new PostCategory()
                .setCategory("Post");
            PostCategory category2 = new PostCategory()
                .setCategory("Archive");
            entityManager.persist(category1);
            entityManager.persist(category2);

            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .addComment(new Comment().setComment("Best book on JPA and Hibernate!").setAuthor("Alice"))
                    .addComment(new Comment().setComment("A must-read for every Java developer!").setAuthor("Bob"))
                    .addComment(new Comment().setComment("A great reference book").setAuthor("Carol"))
                    .addTag(new Tag().setName("JPA").setAuthor("Alice"))
                    .addTag(new Tag().setName("Hibernate").setAuthor("Alice"))
                .addCategory(category1)
                .addCategory(category2)
            );
        });
    }

    @Test
    public void testMerge() {

        PostDTO postDTO = getPostDTO();

        postDTO.addComment(new Comment().setComment("Extra comment").setAuthor("Alice"));
        postDTO.addTag(new Tag().setName("Extra tag").setAuthor("Alice"));
        postDTO.getCategories().remove(postDTO.getCategories().iterator().next());

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            post.setTags(postDTO.getTags());
            post.setComments(postDTO.getComments());
            post.setCategories(postDTO.getCategories());
            entityManager.detach(post);

            Post mergedEntity = entityManager.merge(post);
            entityManager.detach(mergedEntity);

            entityManager.merge(mergedEntity);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals(3, post.getTags().size());
            assertEquals(4, post.getComments().size());
            assertEquals(1, post.getCategories().size());
        });
    }

    private PostDTO getPostDTO() {
        Post post = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p 
                from Post p
                where p.id = :id
                """, Post.class)
                .setParameter("id", 1L)
                .getSingleResult();
        });

        PostDTO postDTO = new PostDTO();
        BeanUtils.copyProperties(post, postDTO);
        return postDTO;
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ElementCollection(fetch = FetchType.EAGER)
        private Set<Comment> comments = new HashSet<>();

        @ElementCollection(fetch = FetchType.EAGER)
        private Set<Tag> tags = new HashSet<>();

        @ManyToMany(fetch = FetchType.EAGER)
        private Set<PostCategory> categories = new HashSet<>();

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public Set<Comment> getComments() {
            return comments;
        }

        public void setComments(Set<Comment> comments) {
            this.comments = comments;
        }

        public Set<Tag> getTags() {
            return tags;
        }

        public void setTags(Set<Tag> tags) {
            this.tags = tags;
        }

        public Set<PostCategory> getCategories() {
            return categories;
        }

        public void setCategories(Set<PostCategory> categories) {
            this.categories = categories;
        }

        public Post addComment(Comment comment) {
            comments.add(comment);
            return this;
        }

        public Post addTag(Tag tag) {
            tags.add(tag);
            return this;
        }

        public Post addCategory(PostCategory category) {
            categories.add(category);
            return this;
        }
    }

    public static class PostDTO {

        private Long id;

        private String title;

        private Set<Comment> comments = new HashSet<>();

        private Set<Tag> tags = new HashSet<>();

        private Set<PostCategory> categories = new HashSet<>();

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

        public Set<Comment> getComments() {
            return comments;
        }

        public void setComments(Set<Comment> comments) {
            this.comments = comments;
        }

        public Set<Tag> getTags() {
            return tags;
        }

        public void setTags(Set<Tag> tags) {
            this.tags = tags;
        }

        public Set<PostCategory> getCategories() {
            return categories;
        }

        public void setCategories(Set<PostCategory> categories) {
            this.categories = categories;
        }

        public PostDTO addComment(Comment comment) {
            comments.add(comment);
            return this;
        }

        public PostDTO addTag(Tag tag) {
            tags.add(tag);
            return this;
        }

        public PostDTO addCategory(PostCategory category) {
            categories.add(category);
            return this;
        }
    }

    @Entity(name = "PostCategory")
    @Table(name = "post_category")
    public static class PostCategory {

        @Id
        @GeneratedValue
        private Long id;

        private String category;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCategory() {
            return category;
        }

        public PostCategory setCategory(String category) {
            this.category = category;
            return this;
        }
    }

    @Embeddable
    public static class Comment implements Serializable {

        private String comment;

        private String author;

        public String getComment() {
            return comment;
        }

        public Comment setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public String getAuthor() {
            return author;
        }

        public Comment setAuthor(String author) {
            this.author = author;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Comment)) return false;
            Comment comment1 = (Comment) o;
            return Objects.equals(comment, comment1.comment) &&
                   Objects.equals(author, comment1.author);
        }

        @Override
        public int hashCode() {
            return Objects.hash(comment, author);
        }
    }

    @Embeddable
    public static class Tag implements Serializable {

        private String name;

        private String author;

        public String getName() {
            return name;
        }

        public Tag setName(String name) {
            this.name = name;
            return this;
        }

        public String getAuthor() {
            return author;
        }

        public Tag setAuthor(String author) {
            this.author = author;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Tag)) return false;
            Tag tag = (Tag) o;
            return Objects.equals(name, tag.name) &&
                   Objects.equals(author, tag.author);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, author);
        }
    }
}
