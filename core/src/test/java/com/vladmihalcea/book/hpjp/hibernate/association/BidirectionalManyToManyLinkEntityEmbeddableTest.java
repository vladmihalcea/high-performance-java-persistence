package com.vladmihalcea.book.hpjp.hibernate.association;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalManyToManyLinkEntityEmbeddableTest
        extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                Tag.class,
                PostTag.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "jcache");
        return properties;
    }

    @Test
    public void testLifecycle() {

        doInJPA(entityManager -> {
            Tag misc = new Tag("Misc");
            Tag jdbc = new Tag("JDBC");
            Tag hibernate = new Tag("Hibernate");
            Tag jooq = new Tag("jOOQ");

            entityManager.persist( misc );
            entityManager.persist( jdbc );
            entityManager.persist( hibernate );
            entityManager.persist( jooq );
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap( Session.class );

            Tag misc = session.bySimpleNaturalId(Tag.class).load( "Misc" );
            Tag jdbc = session.bySimpleNaturalId(Tag.class).load( "JDBC" );
            Tag hibernate = session.bySimpleNaturalId(Tag.class).load( "Hibernate" );
            Tag jooq = session.bySimpleNaturalId(Tag.class).load( "jOOQ" );

            Post hpjp1 = new Post("High-Performance Java Persistence 1st edition");
            hpjp1.setId(1L);

            hpjp1.addTag(jdbc);
            hpjp1.addTag(hibernate);
            hpjp1.addTag(jooq);
            hpjp1.addTag(misc);

            entityManager.persist(hpjp1);

            Post hpjp2 = new Post("High-Performance Java Persistence 2nd edition");
            hpjp2.setId(2L);

            hpjp2.addTag(jdbc);
            hpjp2.addTag(hibernate);
            hpjp2.addTag(jooq);

            entityManager.persist(hpjp2);
        });

        doInJPA(entityManager -> {
            Tag misc = entityManager.unwrap( Session.class )
                .bySimpleNaturalId(Tag.class)
                .load( "Misc" );

            Post post = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.tags pt " +
                "join fetch pt.id.tag " +
                "where p.id = :postId", Post.class)
            .setParameter( "postId", 1L )
            .getSingleResult();

            post.removeTag( misc );
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "id.post", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostTag> tags = new ArrayList<>();

        public Post() {
        }

        public Post(String title) {
            this.title = title;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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
                if (postTag.getId().getTag().equals(tag)) {
                    iterator.remove();
                    postTag.getId().getTag().getPosts().remove(postTag);
                    break;
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

    @Embeddable
    public static class PostTagId
        implements Serializable {

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        @ManyToOne(fetch = FetchType.LAZY)
        private Tag tag;
        
        private PostTagId() {}

        public PostTagId(Post post, Tag tag) {
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
            PostTagId that = (PostTagId) o;
            return Objects.equals(post, that.post) &&
                    Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(post, tag);
        }
    }

    @Entity(name = "PostTag")
    @Table(name = "post_tag")
    public static class PostTag {

        @EmbeddedId
        private PostTagId id;

        @Column(name = "created_on")
        private Date createdOn = new Date();

        private PostTag() {}

        public PostTag(Post post, Tag tag) {
            this.id = new PostTagId(post, tag);
        }

        public PostTagId getId() {
            return id;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostTag that = (PostTag) o;
            return Objects.equals(this.id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id);
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    @NaturalIdCache
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Tag {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String name;

        @OneToMany(
            mappedBy = "id.tag",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
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
