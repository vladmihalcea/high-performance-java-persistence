package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
@RunWith(BytecodeEnhancerRunner.class)
public class EntityGraphTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostDetails.class,
            PostComment.class,
            PostCommentDetails.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            long postId = 1;
            long commentId = 1L;
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(postId++)
                        .setTitle(String.format("High-Performance Java Persistence, part %d", i))
                        .setDetails(
                            new PostDetails()
                                .setCreatedBy("Vlad Mihalcea")
                                .setCreatedOn(new Date())
                        )
                        .addComment(
                            new PostComment()
                                .setId(commentId++)
                                .setReview("The first part is about JDBC")
                                .setDetails(new PostCommentDetails().setViewCount(1))
                        )
                        .addComment(
                            new PostComment()
                                .setId(commentId++)
                                .setReview("The second part is about JPA")
                                .setDetails(new PostCommentDetails().setViewCount(2))
                        )
                        .addComment(
                            new PostComment()
                                .setId(commentId++)
                                .setReview("The third part is about jOOQ")
                                .setDetails(new PostCommentDetails().setViewCount(3))
                        )
                );
            }
        });
    }

    @Test
    public void testFindWithNamedEntityFetchGraph() {
        PostComment comment = doInJPA(entityManager -> {
            EntityGraph<PostComment> postCommentGraph = entityManager.createEntityGraph(PostComment.class);
            postCommentGraph.addAttributeNodes("post");

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
    public void testFindUsingNestedEntityGraph() {
        PostCommentDetails commentDetails = doInJPA(entityManager -> {
            EntityGraph<PostCommentDetails> commentDetailsGraph = entityManager.createEntityGraph(PostCommentDetails.class);
            commentDetailsGraph.addAttributeNodes("comment");
            Subgraph<PostComment> commentSubgraph = commentDetailsGraph.addSubgraph("comment");
            commentSubgraph.addAttributeNodes("post");

            return entityManager.find(PostCommentDetails.class, 1L,
                Collections.singletonMap(
                    "jakarta.persistence.loadgraph",
                    commentDetailsGraph
                )
            );
        });

        assertNotNull(commentDetails.getComment());
        assertNotNull(commentDetails.getComment().getPost());
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

    @Test
    public void testFindPostWithAllAssociations() {
        Post post = doInJPA(entityManager -> {
            return entityManager.find(Post.class, 1L,
                Collections.singletonMap(
                    "jakarta.persistence.fetchgraph",
                    entityManager.getEntityGraph("Post.all")
                )
            );
        });

        assertEquals("High-Performance Java Persistence, part 1", post.getTitle());
        assertEquals("Vlad Mihalcea", post.getDetails().getCreatedBy());
        assertEquals(3, post.getComments().size());
        assertTrue(post.getComments().get(0).getDetails().getViewCount() > 0);
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @NamedEntityGraph(
        name = "Post.details",
        attributeNodes = @NamedAttributeNode("details")
    )
    @NamedEntityGraph(
        name = "Post.all",
        attributeNodes = {
            @NamedAttributeNode("details"),
            @NamedAttributeNode(value = "comments", subgraph = "Post.comment.details"),
        },
        subgraphs = @NamedSubgraph(
            name = "Post.comment.details",
            attributeNodes = @NamedAttributeNode("details")
        )
    )
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
    @NamedEntityGraph(
        name = "PostComment.post",
        attributeNodes = @NamedAttributeNode("post")
    )
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        @OneToOne(mappedBy = "comment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
        private PostCommentDetails details;

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

        public PostCommentDetails getDetails() {
            return details;
        }

        public PostComment setDetails(PostCommentDetails details) {
            if (details == null) {
                if (this.details != null) {
                    this.details.setComment(null);
                }
            }
            else {
                details.setComment(this);
            }
            this.details = details;
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

    @Entity(name = "PostCommentDetails")
    @Table(name = "post_comment_details")
    public static class PostCommentDetails {

        @Id
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        @JoinColumn(name = "id")
        private PostComment comment;

        @Column(name = "view_count")
        private int viewCount;

        public Long getId() {
            return id;
        }

        public PostCommentDetails setId(Long id) {
            this.id = id;
            return this;
        }

        public PostComment getComment() {
            return comment;
        }

        public PostCommentDetails setComment(PostComment comment) {
            this.comment = comment;
            return this;
        }

        public int getViewCount() {
            return viewCount;
        }

        public PostCommentDetails setViewCount(int views) {
            this.viewCount = views;
            return this;
        }
    }
}
