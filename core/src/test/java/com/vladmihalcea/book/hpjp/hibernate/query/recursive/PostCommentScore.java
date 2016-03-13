package com.vladmihalcea.book.hpjp.hibernate.query.recursive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * <code>PostCommentScore</code> - PostComment Score
 *
 * @author Vlad Mihalcea
 */
public class PostCommentScore {

    private final Long id;
    private final Long parentId;
    private final Long rootId;
    private final String review;
    private final Date createdOn;
    private final long score;

    private List<PostCommentScore> children = new ArrayList<>();

    public PostCommentScore(Number id, Number parentId, Number rootId, String review, Date createdOn, Number score) {
        this.id = id.longValue();
        this.parentId = parentId != null ? parentId.longValue() : null;
        this.rootId = rootId.longValue();
        this.review = review;
        this.createdOn = createdOn;
        this.score = score.longValue();
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getRootId() {
        return rootId;
    }

    public String getReview() {
        return review;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public long getScore() {
        return score;
    }

    public long getTotalScore() {
        long total = getScore();
        for(PostCommentScore child : children) {
            total += child.getTotalScore();
        }
        return total;
    }

    public List<PostCommentScore> getChildren() {
        return children;
    }

    public void addChild(PostCommentScore child) {
        children.add(child);
        children.sort(Comparator.comparing(PostCommentScore::getCreatedOn));
    }
}
