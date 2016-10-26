package com.vladmihalcea.book.hpjp.hibernate.query.recursive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentScore {

    private Long id;
    private Long parentId;
    private String review;
    private Date createdOn;
    private long score;

    private List<PostCommentScore> children = new ArrayList<>();

    public PostCommentScore(Number id, Number parentId, String review, Date createdOn, Number score) {
        this.id = id.longValue();
        this.parentId = parentId != null ? parentId.longValue() : null;
        this.review = review;
        this.createdOn = createdOn;
        this.score = score.longValue();
    }

    public PostCommentScore() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public long getTotalScore() {
        long total = getScore();
        for(PostCommentScore child : children) {
            total += child.getTotalScore();
        }
        return total;
    }

    public List<PostCommentScore> getChildren() {
        List<PostCommentScore> copy = new ArrayList<>(children);
        copy.sort(Comparator.comparing(PostCommentScore::getCreatedOn));
        return copy;
    }

    public void addChild(PostCommentScore child) {
        children.add(child);
    }
}
