package com.vladmihalcea.hpjp.spring.data.update.domain;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
public class Post {

    @Id
    private Long id;

    private String title;

    private long likes;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Timestamp createdOn;

    @Transient
    private String creationTimestamp;

    public Post() {
        this.createdOn = new Timestamp(System.currentTimeMillis());
    }

    public String getCreationTimestamp() {
        if(creationTimestamp == null) {
            creationTimestamp = DateTimeFormatter.ISO_DATE_TIME.format(
                createdOn.toLocalDateTime()
            );
        }
        return creationTimestamp;
    }

    @Override
    public String toString() {
        return String.format("""
                Post{
                  id=%d
                  title='%s'
                  likes=%d
                  creationTimestamp='%s'
                }"""
            , id, title, likes, getCreationTimestamp()
        );
    }

    public Long getId() {
        return id;
    }

    public Post setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Post setTitle(String title) {
        this.title = title;
        return this;
    }

    public long getLikes() {
        return likes;
    }

    public Post setLikes(long likes) {
        this.likes = likes;
        return this;
    }

    public Timestamp getCreatedOn() {
        return createdOn;
    }
}
