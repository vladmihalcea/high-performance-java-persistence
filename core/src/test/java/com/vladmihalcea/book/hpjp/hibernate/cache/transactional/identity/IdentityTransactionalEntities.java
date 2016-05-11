package com.vladmihalcea.book.hpjp.hibernate.cache.transactional.identity;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class IdentityTransactionalEntities {

    @Entity(name = "Post")
    @Table(name = "post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String title;

        @Version
        private int version;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
        @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    public static class PostComment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

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
    }
}
