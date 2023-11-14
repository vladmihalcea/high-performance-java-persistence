package com.vladmihalcea.hpjp.spring.data.unidirectional.domain;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post_comments")
public class PostComment extends VersionedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        foreignKey = @ForeignKey(
            name = "FK_post_comment_post_id"
        )
    )
    private Post post;

    @Column(length = 250)
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
