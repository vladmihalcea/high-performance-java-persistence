package com.vladmihalcea.hpjp.spring.data.query.method.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentDTO {

    public static final String ID = "id";
    public static final String POST_ID = "postId";
    public static final String PARENT_ID = "parentId";
    public static final String REVIEW = "review";
    public static final String CREATED_ON = "createdOn";
    public static final String VOTES = "votes";

    private Long id;

    private Long postId;

    private Long parentId;

    private String review;

    private Date createdOn;

    private int votes;

    @JsonIgnore
    private PostCommentDTO parent;

    private List<PostCommentDTO> replies = new ArrayList<>();

    public PostCommentDTO(Object[] tuples, Map<String, Integer> aliasToIndexMap) {
        this.id = (Long) tuples[aliasToIndexMap.get(ID)];
        this.postId = (Long) tuples[aliasToIndexMap.get(POST_ID)];
        this.parentId = (Long) tuples[aliasToIndexMap.get(PARENT_ID)];
        this.review = (String) tuples[aliasToIndexMap.get(REVIEW)];
        this.createdOn = Timestamp.valueOf((LocalDateTime) tuples[aliasToIndexMap.get(CREATED_ON)]);
        this.votes = (int) tuples[aliasToIndexMap.get(VOTES)];
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
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

    public int getVotes() {
        return votes;
    }

    public List<PostCommentDTO> getReplies() {
        return replies;
    }

    public void addReply(PostCommentDTO reply) {
        replies.add(reply);
        reply.parent = this;
    }

    public PostCommentDTO getParent() {
        return parent;
    }

    public PostCommentDTO root() {
        return (parent != null) ? parent.root() : this;
    }
}
