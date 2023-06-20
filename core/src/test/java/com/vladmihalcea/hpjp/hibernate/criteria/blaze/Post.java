package com.vladmihalcea.hpjp.hibernate.criteria.blaze;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link com.vladmihalcea.hpjp.hibernate.criteria.blaze.Post}
 *
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
public class Post {

    @Id
    private Long id;

    private String title;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "post",
        orphanRemoval = true, fetch = FetchType.LAZY)
    private BlazePersistenceCriteriaTest.PostDetails details;

    @ManyToMany
    @JoinTable(name = "post_tag",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<BlazePersistenceCriteriaTest.Tag> tags = new ArrayList<>();

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

    private Post setComments(List<PostComment> comments) {
        this.comments = comments;
        return this;
    }

    public Post addComment(PostComment comment) {
        comments.add(comment);
        comment.setPost(this);

        return this;
    }

    public Post removeComment(PostComment comment) {
        comments.remove(comment);
        comment.setPost(null);

        return this;
    }

    public Post addDetails(BlazePersistenceCriteriaTest.PostDetails details) {
        this.details = details;
        details.setPost(this);

        return this;
    }

    public Post removeDetails() {
        this.details.setPost(null);
        this.details = null;

        return this;
    }

    public List<BlazePersistenceCriteriaTest.Tag> getTags() {
        return tags;
    }
}
