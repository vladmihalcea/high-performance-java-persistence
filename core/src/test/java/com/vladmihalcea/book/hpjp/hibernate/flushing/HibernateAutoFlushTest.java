package com.vladmihalcea.book.hpjp.hibernate.flushing;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HibernateAutoFlushTest extends JPAAutoFlushTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostDetails.class,
            Tag.class
        };
    }

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return true;
    }

    @Test
    public void testFlushAutoNativeSQL() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager
                    .createNativeQuery(
                        "select count(*) " +
                        "from post")
                    .getSingleResult()
                ).intValue()
            );

            Post post = new Post("High-Performance Java Persistence");

            entityManager.persist(post);

            int postCount = ((Number)
            entityManager
            .createNativeQuery(
                "select count(*) " +
                "from post")
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoNativeSQLFlushModeAlways() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager
                    .createNativeQuery(
                        "select count(*) " +
                        "from post")
                    .getSingleResult()
                ).intValue()
            );

            Post post = new Post("High-Performance Java Persistence");

            entityManager.persist(post);

            int postCount = ((Number)
            entityManager
            .unwrap(Session.class)
            .createNativeQuery(
                "select count(*) " +
                "from post")
            .setFlushMode(FlushMode.ALWAYS)
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoNativeSQLSynchronizedEntityClass() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager
                    .createNativeQuery(
                        "select count(*) " +
                        "from post")
                    .getSingleResult()
                ).intValue()
            );

            Post post = new Post("High-Performance Java Persistence");

            entityManager.persist(post);

            int postCount = ((Number)
            entityManager
            .unwrap(Session.class)
            .createNativeQuery(
                "select count(*) " +
                "from post")
            .addSynchronizedEntityClass(Post.class)
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Test
    public void testFlushAutoNativeSQLSynchronizedQuerySpace() {
        doInJPA(entityManager -> {
            assertEquals(
                0,
                ((Number)
                    entityManager
                    .createNativeQuery(
                        "select count(*) " +
                        "from post")
                    .getSingleResult()
                ).intValue()
            );

            Post post = new Post("High-Performance Java Persistence");

            entityManager.persist(post);

            int postCount = ((Number)
            entityManager
            .unwrap(Session.class)
            .createNativeQuery(
                "select count(*) " +
                "from post")
            .addSynchronizedQuerySpace("post")
            .getSingleResult()).intValue();

            assertEquals(1, postCount);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToOne(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
        )
        private PostDetails details;

        @ManyToMany
        @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private Set<Tag> tags = new HashSet<>();

        public Post() {}

        public Post(String title) {
            this.title = title;
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        public PostDetails() {
            createdOn = new Date();
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
    }
}
