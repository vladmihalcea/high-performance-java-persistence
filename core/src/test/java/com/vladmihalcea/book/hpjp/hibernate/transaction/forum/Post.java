package com.vladmihalcea.book.hpjp.hibernate.transaction.forum;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue
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

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "post",
            orphanRemoval = true, fetch = FetchType.LAZY)
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

    public List<PostComment> getComments() {
        return comments;
    }

    public PostDetails getDetails() {
        return details;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public Post addComment(PostComment comment) {
        comments.add(comment);
        comment.setPost(this);

        return this;
    }

    public Post addDetails(PostDetails details) {
        this.details = details;
        details.setPost(this);

        return this;
    }

    public Post removeDetails() {
        this.details.setPost(null);
        this.details = null;

        return this;
    }
}
