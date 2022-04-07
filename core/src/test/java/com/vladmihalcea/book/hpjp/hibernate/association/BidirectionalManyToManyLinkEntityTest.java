package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalManyToManyLinkEntityTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                Tag.class,
                PostTag.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Tag().setName("JPA")
            );

            entityManager.persist(
                new Tag().setName("Hibernate")
            );
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);

            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("JPA with Hibernate")
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("JPA"))
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("Hibernate"))
            );

            entityManager.persist(
                new Post()
                    .setId(2L)
                    .addTag(session.bySimpleNaturalId(Tag.class).getReference("Hibernate"))
            );
        });
    }

    @Test
    public void testRemoveTagReference() {
        doInJPA(entityManager -> {
            Post post1 = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.tags
                where p.id = :id
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            Session session = entityManager.unwrap(Session.class);

            post1.removeTag(session.bySimpleNaturalId(Tag.class).getReference("JPA"));
        });
    }

    @Test
    public void testRemovePostEntity() {
        doInJPA(entityManager -> {
            LOGGER.info("Remove");
            Post post1 = entityManager.getReference(Post.class, 1L);

            entityManager.remove(post1);
        });
    }

    @Test
    public void testShuffle() {
        doInJPA(entityManager -> {
            LOGGER.info("Shuffle");
            Post post1 = entityManager.find(Post.class, 1L);

            post1.getTags().sort(
                Comparator.comparing((PostTag postTag) -> postTag.getId().getTagId())
                .reversed()
            );
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostTag> tags = new ArrayList<>();

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

        public List<PostTag> getTags() {
            return tags;
        }

        public Post addTag(Tag tag) {
            PostTag postTag = new PostTag(this, tag);
            tags.add(postTag);
            tag.getPosts().add(postTag);
            return this;
        }

        public Post removeTag(Tag tag) {
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
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Post)) return false;
            return id != null && id.equals(((Post) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    @Embeddable
    public static class PostTagId implements Serializable {

        private Long postId;

        private Long tagId;

        private PostTagId() {}

        public PostTagId(Long postId, Long tagId) {
            this.postId = postId;
            this.tagId = tagId;
        }

        public Long getPostId() {
            return postId;
        }

        public Long getTagId() {
            return tagId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostTagId that = (PostTagId) o;
            return Objects.equals(postId, that.getPostId()) &&
                    Objects.equals(tagId, that.getTagId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, tagId);
        }
    }

    @Entity(name = "PostTag")
    @Table(name = "post_tag")
    public static class PostTag {

        @EmbeddedId
        private PostTagId id;

        @ManyToOne
        @MapsId("postId")
        private Post post;

        @ManyToOne
        @MapsId("tagId")
        private Tag tag;

        private PostTag() {}

        public PostTag(Post post, Tag tag) {
            this.post = post;
            this.tag = tag;
            this.id = new PostTagId(post.getId(), tag.getId());
        }

        public PostTagId getId() {
            return id;
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
    @Table(name = "tag")
    public static class Tag {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String name;

        @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostTag> posts = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public Tag setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Tag setName(String name) {
            this.name = name;
            return this;
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
