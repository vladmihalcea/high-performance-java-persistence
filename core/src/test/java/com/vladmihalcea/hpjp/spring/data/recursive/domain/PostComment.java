package com.vladmihalcea.hpjp.spring.data.recursive.domain;

import jakarta.persistence.*;

import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "PostComment")
@Table(name = "post_comment")
@SqlResultSetMapping(
    name = "PostCommentDTO",
    classes = @ConstructorResult(
        targetClass = PostCommentDTO.class,
        columns = {
            @ColumnResult(name = "id"),
            @ColumnResult(name = "parent_id"),
            @ColumnResult(name = "review"),
            @ColumnResult(name = "created_on"),
            @ColumnResult(name = "score")
        }
    )
)
public class PostComment {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private PostComment parent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on")
    private Date createdOn;

    private String review;

    private int score;

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

    public PostComment getParent() {
        return parent;
    }

    public PostComment setParent(PostComment parent) {
        this.parent = parent;
        return this;
    }

    public String getReview() {
        return review;
    }

    public PostComment setReview(String review) {
        this.review = review;
        return this;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public PostComment setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public int getScore() {
        return score;
    }

    public PostComment setScore(int score) {
        this.score = score;
        return this;
    }
}
