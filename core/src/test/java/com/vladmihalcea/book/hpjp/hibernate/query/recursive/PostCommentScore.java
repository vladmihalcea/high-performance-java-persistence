package com.vladmihalcea.book.hpjp.hibernate.query.recursive;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <code>PostCommentScore</code> - PostComment Score
 *
 * @author Vlad Mihalcea
 */
public class PostCommentScore {

    private final Number id;
    private final Number parentId;
    private final Number rootId;
    private final String review;
    private final Date createdOn;
    private final Number totalScore;

    private List<PostCommentScore> children = new ArrayList<>();

    public PostCommentScore(Number id, Number parentId, Number rootId, String review, Date createdOn, Number totalScore) {
        this.id = id;
        this.parentId = parentId;
        this.rootId = rootId;
        this.review = review;
        this.createdOn = createdOn;
        this.totalScore = totalScore;
    }

    public Number getId() {
        return id;
    }

    public Number getParentId() {
        return parentId;
    }

    public Number getRootId() {
        return rootId;
    }

    public String getReview() {
        return review;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Number getTotalScore() {
        return totalScore;
    }

    public List<PostCommentScore> getChildren() {
        return children;
    }
}
