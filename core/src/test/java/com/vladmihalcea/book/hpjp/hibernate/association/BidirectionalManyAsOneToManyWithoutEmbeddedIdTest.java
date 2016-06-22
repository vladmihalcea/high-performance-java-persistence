package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalManyAsOneToManyWithoutEmbeddedIdTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                Tag.class,
                PostTag.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post1 = new Post("JPA with Hibernate");
            Post post2 = new Post("Native Hibernate");

            Tag tag1 = new Tag("Java");
            Tag tag2 = new Tag("Hibernate");

            entityManager.persist(post1);
            entityManager.persist(post2);

            entityManager.persist(tag1);
            entityManager.persist(tag2);

            post1.addTag(tag1);
            post1.addTag(tag2);

            post2.addTag(tag1);

            entityManager.flush();

            LOGGER.info("Remove");
            post1.removeTag(tag1);
        });
    }

    @Test
    public void testShuffle() {
        final Long postId = doInJPA(entityManager -> {
            Post post1 = new Post("JPA with Hibernate");
            Post post2 = new Post("Native Hibernate");

            Tag tag1 = new Tag("Java");
            tag1.setId(1L);
            Tag tag2 = new Tag("Hibernate");
            tag2.setId(2L);

            entityManager.persist(post1);
            entityManager.persist(post2);

            entityManager.persist(tag1);
            entityManager.persist(tag2);

            post1.addTag(tag1);
            post1.addTag(tag2);

            post2.addTag(tag1);

            entityManager.flush();

            return post1.getId();
        });
        doInJPA(entityManager -> {
            LOGGER.info("Shuffle");
            Post post1 = entityManager.find(Post.class, postId);
            Tag tag1 = entityManager.find(Tag.class, 1L);

            PostTag postTag = entityManager.find(PostTag.class, new PostTag(post1, tag1));

            post1.getTags().sort((postTag1, postTag2) ->
                postTag2.getTag().getId().compareTo(postTag1.getTag().getId())
            );
        });
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostTag> tags = new ArrayList<>();

        public Post() {
        }

        public Post(String title) {
            this.title = title;
        }

        public Long getId() {
            return id;
        }

        public List<PostTag> getTags() {
            return tags;
        }

        public void addTag(Tag tag) {
            PostTag postTag = new PostTag(this, tag);
            tags.add(postTag);
            tag.getPosts().add(postTag);
        }

        public void removeTag(Tag tag) {
            for (Iterator<PostTag> iterator = tags.iterator(); iterator.hasNext(); ) {
                PostTag postTag = iterator.next();
                if (postTag.getPost().equals(this) &&
                        postTag.getTag().equals(tag)) {
                    iterator.remove();
                    postTag.getTag().getPosts().remove(postTag);
                    postTag.setPost(null);
                    postTag.setTag(null);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Post post = (Post) o;
            return Objects.equals(title, post.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title);
        }
    }

    @Entity(name = "PostTag") @Table(name = "post_tag")
    public static class PostTag implements Serializable {

        @Id
        @ManyToOne
        private Post post;

        @Id
        @ManyToOne
        private Tag tag;

        private PostTag() {}

        public PostTag(Post post, Tag tag) {
            this.post = post;
            this.tag = tag;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public Tag getTag() {
            return tag;
        }

        public void setTag(Tag tag) {
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostTag that = (PostTag) o;
            return Objects.equals(post, that.post) &&
                    Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(post, tag);
        }
    }

    @Entity(name = "Tag")
    public static class Tag {

        @Id
        private Long id;

        private String name;

        @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostTag> posts = new ArrayList<>();

        public Tag() {
        }

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

        public List<PostTag> getPosts() {
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
