package com.vladmihalcea.book.hpjp.hibernate.cache.query;

import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
public class PostSummary {

    private Long id;

    private String title;

    private Date createdOn;

    private int commentCount;

    public PostSummary(Long id, String title, Date createdOn, Number commentCount) {
        this.id = id;
        this.title = title;
        this.createdOn = createdOn;
        this.commentCount = commentCount.intValue();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public int getCommentCount() {
        return commentCount;
    }

    @Override
    public String toString() {
        return "PostSummary{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", createdOn=" + createdOn +
                ", commentCount=" + commentCount +
                '}';
    }
}
