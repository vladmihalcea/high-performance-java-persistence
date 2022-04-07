package com.vladmihalcea.book.hpjp.util.providers.entity;

import com.vladmihalcea.book.hpjp.util.EntityProvider;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class PostDetailsCommentsEntityProvider implements EntityProvider {

        @Override
        public Class<?>[] entities() {
            return new Class<?>[]{
                    Post.class,
                    PostDetails.class,
                    PostComment.class
            };
        }

        @Entity(name = "Post")
        @Table(name = "post")
        public static class Post {

            @Id
            private Long id;

            private String title;

            @Version
            private int version;

            public Post() {
            }

            public Post(Long id) {
                this.id = id;
            }

            public Post(String title) {
                this.title = title;
            }

            @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                    orphanRemoval = true)
            private List<PostComment> comments = new ArrayList<>();

            @OneToOne(cascade = CascadeType.ALL, mappedBy = "post",
                    orphanRemoval = true, fetch = FetchType.LAZY)
            private PostDetails details;

            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public int getVersion() {
                return version;
            }

            public void setVersion(int version) {
                this.version = version;
            }

            public List<PostComment> getComments() {
                return comments;
            }

            public PostDetails getDetails() {
                return details;
            }

            public void addComment(PostComment comment) {
                comments.add(comment);
                comment.setPost(this);
            }

            public void addDetails(PostDetails details) {
                this.details = details;
                details.setPost(this);
            }

            public void removeDetails() {
                this.details.setPost(null);
                this.details = null;
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

            @Version
            private int version;

            public PostDetails() {
                createdOn = new Date();
            }

            @OneToOne(fetch = FetchType.LAZY)
            @MapsId
            private Post post;

            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public Post getPost() {
                return post;
            }

            public void setPost(Post post) {
                this.post = post;
            }

            public Date getCreatedOn() {
                return createdOn;
            }

            public void setCreatedOn(Date createdOn) {
                this.createdOn = createdOn;
            }

            public String getCreatedBy() {
                return createdBy;
            }

            public void setCreatedBy(String createdBy) {
                this.createdBy = createdBy;
            }

            public int getVersion() {
                return version;
            }

            public void setVersion(int version) {
                this.version = version;
            }
        }

        @Entity(name = "PostComment")
        @Table(name = "post_comment")
        public static class PostComment {

            @Id
            private Long id;

            @ManyToOne
            private Post post;

            @Version
            private int version;

            private String review;

            public PostComment() {
            }

            public PostComment(String review) {
                this.review = review;
            }


            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public Post getPost() {
                return post;
            }

            public void setPost(Post post) {
                this.post = post;
            }

            public String getReview() {
                return review;
            }

            public void setReview(String review) {
                this.review = review;
            }

            public int getVersion() {
                return version;
            }

            public void setVersion(int version) {
                this.version = version;
            }
        }
    }