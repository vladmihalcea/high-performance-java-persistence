package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;
import org.springframework.beans.BeanUtils;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ElementCollectionListMergeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostCategory.class
        };
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
                    .addCategory(category1)
                    .addCategory(category2)
                    .addComment(new Comment().setComment("firstComment"))
            );
        });
    }

    @Test
    public void testMerge() {

        PostDTO postDTO = getPostDTO();

        doInJPA(entityManager -> {
            //second find and copy from dto
            Post post = entityManager.find(Post.class, 1L);
            BeanUtils.copyProperties(postDTO, post);

            // find posts by category
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                join p.categories c
                where c.id = :categoryId
                """, Post.class)
                .setParameter("categoryId", post.categories.iterator().next().id)
                .getResultList();

            // update post
            post = entityManager.find(Post.class, 1L);
            BeanUtils.copyProperties(postDTO, post);
            Object mergedEntity = update(entityManager, post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals(2, post.getTags().size());
            assertEquals(3, post.getComments().size());
            assertEquals(2, post.getCategories().size());
        });
    }

    private PostDTO getPostDTO() {
        Post post = doInJPA(entityManager -> {
            return entityManager.find(Post.class, 1L);
        });

        PostDTO postDTO = new PostDTO();
        postDTO.id = post.id;
        postDTO.title = post.title;
        postDTO.categories = post.categories;
        postDTO.tags = post.tags;
        postDTO.comments = new HashSet<>();
        postDTO
            .addComment(new Comment().setComment("Best book on JPA and Hibernate!").setAuthor("Alice"))
            .addComment(new Comment().setComment("A must-read for every Java developer!").setAuthor("Bob"))
            .addComment(new Comment().setComment("A great reference book").setAuthor("Carol"))
            .addTag(new Tag().setName("JPA").setAuthor("Alice"))
            .addTag(new Tag().setName("Hibernate").setAuthor("Alice"));
        return postDTO;
    }

    private Object update(EntityManager entityManager, Object object) {
        Object mergedEntity = entityManager.merge(object);
        entityManager.detach(object);
        Object merged2 = entityManager.merge(mergedEntity);
        return merged2;
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(
            name = "post_comment",
            joinColumns = @JoinColumn(name = "post_id")
        )
        private List<Comment> comments = new ArrayList<>();

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(
            name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id")
        )
        private Set<Tag> tags = new HashSet<>();

        @ManyToMany(fetch = FetchType.EAGER)
        @JoinTable(
            name = "post_categories",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "post_id")
        )
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

        public List<Comment> getComments() {
            return comments;
        }

        public void setComments(List<Comment> comments) {
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
            return comment.equals(comment1.comment) &&
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
