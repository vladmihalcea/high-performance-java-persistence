package com.vladmihalcea.book.hpjp.hibernate.association;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return Database.POSTGRESQL;
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
                    .addComment("Best book on JPA and Hibernate!")
                    .addComment("A must-read for every Java developer!")
                    .addComment("A great reference book")
                    .addTag("JPA")
                    .addTag("Hibernate")
                .addCategory(category1)
                .addCategory(category2)
            );
        });
    }

    @Test
    public void testMerge() {

        Post detachedPost = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p 
                from Post p
                where p.id = :id
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();
        });

        detachedPost.addComment("Extra comment");
        detachedPost.addTag("Extra tag");
        detachedPost.getCategories().remove(detachedPost.getCategories().iterator().next());

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            post.setTags(detachedPost.getTags());
            post.setComments(detachedPost.getComments());
            post.setCategories(detachedPost.getCategories());
            entityManager.detach(post);

            Post mergedEntity = entityManager.merge(post);
            entityManager.detach(mergedEntity);

            Session session = entityManager.unwrap(Session.class);
            session.update(mergedEntity);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals(3, post.getTags().size());
            assertEquals(4, post.getComments().size());
            assertEquals(1, post.getCategories().size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ElementCollection(fetch = FetchType.EAGER)
        private Set<String> comments = new HashSet<>();

        @ElementCollection(fetch = FetchType.EAGER)
        private Set<String> tags = new HashSet<>();

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

        public Set<String> getComments() {
            return comments;
        }

        public void setComments(Set<String> comments) {
            this.comments = comments;
        }

        public Set<String> getTags() {
            return tags;
        }

        public void setTags(Set<String> tags) {
            this.tags = tags;
        }

        public Post addComment(String comment) {
            comments.add(comment);
            return this;
        }

        public Post addTag(String tag) {
            tags.add(tag);
            return this;
        }

        public Set<PostCategory> getCategories() {
            return categories;
        }

        public void setCategories(Set<PostCategory> categories) {
            this.categories = categories;
        }

        public Post addCategory(PostCategory category) {
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
}
