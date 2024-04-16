package com.vladmihalcea.hpjp.hibernate.association;

import com.vladmihalcea.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToOneNPlusOneNonOptionalTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 100; i++) {
                Post post = new Post().setTitle(String.format("Post nr. %d", i));
                post.setDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"));

                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testNoNPlusOne() {
        SQLStatementCountValidator.reset();

        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p
                from Post p
                where p.title like 'Post nr.%'
                """, Post.class)
            .getResultList();
        });

        assertEquals(100, posts.size());
        SQLStatementCountValidator.assertSelectCount(1);
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
            fetch = FetchType.LAZY,
            optional = false
        )
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
            if (details == null) {
                if (this.details != null) {
                    this.details.setPost(null);
                }
            }
            else {
                details.setPost(this);
            }
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

        public Date getCreatedOn() {
            return createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public PostDetails setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostDetails setPost(Post post) {
            this.post = post;
            return this;
        }
    }
}
