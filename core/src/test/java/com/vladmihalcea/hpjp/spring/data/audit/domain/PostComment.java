package com.vladmihalcea.hpjp.spring.data.audit.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post_comment")
@Audited
public class PostComment {

    @Id
    @GeneratedValue
    private Long id;

    @Column(length = 250)
    private String review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_POST_COMMENT_POST_ID"))
    private Post post;

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
