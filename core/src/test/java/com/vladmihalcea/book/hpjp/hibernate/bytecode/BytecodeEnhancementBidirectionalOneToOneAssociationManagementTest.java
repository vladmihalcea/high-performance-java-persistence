package com.vladmihalcea.book.hpjp.hibernate.bytecode;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;
import java.util.Date;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(
    biDirectionalAssociationManagement = true
)
public class BytecodeEnhancementBidirectionalOneToOneAssociationManagementTest extends AbstractTest {

    //Needed as otherwise we get a No unique field [LOGGER] error
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostDetails.class
        };
    }

    @Test
    public void testSetParentAssociation() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");

            PostDetails details = new PostDetails()
                .setCreatedBy("Vlad Mihalcea");

            assertNull(details.getPost());
            post.setDetails(details);
            assertSame(post, details.getPost());

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            PostDetails details = entityManager.find(PostDetails.class, 1L);
            assertEquals("High-Performance Java Persistence", details.getPost().getTitle());
            assertEquals("Vlad Mihalcea", details.getCreatedBy());
        });
    }

    @Test
    public void testSetChildAssociation() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");

            PostDetails details = new PostDetails()
                .setCreatedBy("Vlad Mihalcea");

            assertNull(post.getDetails());
            details.setPost(post);
            assertSame(details, post.getDetails());

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            PostDetails details = entityManager.find(PostDetails.class, 1L);

            assertEquals("High-Performance Java Persistence", details.getPost().getTitle());
            assertEquals("Vlad Mihalcea", details.getCreatedBy());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToOne(
            mappedBy = "post",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL
        )
        @LazyToOne(LazyToOneOption.NO_PROXY)
        private PostDetails details;

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

        public PostDetails getDetails() {
            return details;
        }

        public Post setDetails(PostDetails details) {
            this.details = details;
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
