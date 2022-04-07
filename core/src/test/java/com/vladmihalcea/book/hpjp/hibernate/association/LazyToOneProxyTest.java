package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class LazyToOneProxyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostDetails.class
        };
    }

    @Test
    public void testLazyLoadingNoProxy() {
        final Post post = new Post()
            .setId(1L)
            .setTitle("High-Performance Java Persistence, 1st Part");

        doInJPA(entityManager -> {
            entityManager.persist(post);

            entityManager.persist(
                new PostDetails()
                    .setPost(post)
                    .setCreatedBy("Vlad Mihalcea")
            );
        });

        PostDetails details = doInJPA(entityManager -> {
            return entityManager.find(PostDetails.class, post.getId());
        });

        assertNotNull(details.getPost());
        LOGGER.info("PostDetail Proxy class: {}", details.getPost().getClass());
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn = new Date();

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne(fetch = FetchType.LAZY)
        @LazyToOne(LazyToOneOption.PROXY)
        @MapsId
        @JoinColumn(name = "id")
        private Post post;

        public Long getId() {
            return id;
        }

        public PostDetails setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostDetails setPost(Post post) {
            this.post = post;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public PostDetails setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public PostDetails setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
    }
}
