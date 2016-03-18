package com.vladmihalcea.book.hpjp.hibernate.query.recursive.simple;

import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "PostComment")
@Table(name = "post_comment")
@SqlResultSetMapping(
        name = "PostCommentScore",
        classes = @ConstructorResult(
                targetClass = PostCommentScore.class,
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
    private Date createdOn = new Date();

    private String review;

    private int score;

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

    public PostComment getParent() {
        return parent;
    }

    public void setParent(PostComment parent) {
        this.parent = parent;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
