package com.vladmihalcea.hpjp.spring.data.recursive.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentDTO {

    private Long id;
    private Long parentId;
    private String review;
    private Date createdOn;
    private long score;

    private PostCommentDTO parent;

    private List<PostCommentDTO> children = new ArrayList<>();

    public PostCommentDTO(Number id, Number parentId, String review, Date createdOn, Number score) {
        this.id = id.longValue();
        this.parentId = parentId != null ? parentId.longValue() : null;
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
        for (PostCommentDTO child : children) {
            total += child.getTotalScore();
        }
        return total;
    }

    public List<PostCommentDTO> getChildren() {
        List<PostCommentDTO> copy = new ArrayList<>(children);
        copy.sort(Comparator.comparing(PostCommentDTO::getCreatedOn));
        return copy;
    }

    public void addChild(PostCommentDTO child) {
        children.add(child);
        child.parent = this;
    }

    public PostCommentDTO getRoot() {
        if(parent != null) {
            return parent.getRoot();
        }
        return this;
    }
}
