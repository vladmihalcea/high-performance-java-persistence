package com.vladmihalcea.book.hpjp.hibernate.forum;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post")
public class Post {

    @Id
    private Long id;

    private String title;

    public Post() {
    }

    public Post(Long id) {
        this.id = id;
    }

    public Post(String title) {
        this.title = title;
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
            orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "post", fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.NO_PROXY)
    private PostDetails details;

    @ManyToMany
    @JoinTable(name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<PostComment> getComments() {
        return comments;
    }

    public PostDetails getDetails() {
        return details;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void addComment(PostComment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public void addDetails(PostDetails details) {
        this.details = details;
        details.setPost(this);
    }

    public void removeDetails() {
        this.details.setPost(null);
        this.details = null;
    }
}
