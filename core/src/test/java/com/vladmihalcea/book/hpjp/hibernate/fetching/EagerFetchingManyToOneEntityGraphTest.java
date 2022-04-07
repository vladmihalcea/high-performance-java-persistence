package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class EagerFetchingManyToOneEntityGraphTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
            PostDetails.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i++)
                        .setTitle(String.format("High-Performance Java Persistence, part %d", i))
                        .setDetails(
                            new PostDetails()
                                .setCreatedBy("Vlad Mihalcea")
                                .setCreatedOn(new Date())
                        )
                        .addComment(
                            new PostComment()
                                .setReview("The first part is about JDBC")
                        )
                        .addComment(
                            new PostComment()
                                .setReview("The second part is about JPA")
                        )
                        .addComment(
                            new PostComment()
                                .setReview("The third part is about jOOQ")
                        )
                );
            }
        });
    }

    @Test
    public void testFindWithNamedEntityFetchGraph() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L,
                Collections.singletonMap(
                    "jakarta.persistence.fetchgraph",
                    entityManager.getEntityGraph("PostComment.post")
                )
            );
        });
        assertNotNull(comment.getPost());
    }

    @Test
    public void testFindWithNamedEntityLoadGraph() {
        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L,
                Collections.singletonMap(
                    "jakarta.persistence.loadgraph",
                    entityManager.getEntityGraph("PostComment.post")
                )
            );
        });
        assertNotNull(comment.getPost());
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;
        
        @OneToOne(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
        )
        private PostDetails details;

        @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
        )
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
        }
    }

    @Entity(name = "PostDetails")
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

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @NamedEntityGraph(name = "PostComment.post", attributeNodes = @NamedAttributeNode("post"))
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }
}
