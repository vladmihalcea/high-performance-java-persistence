package com.vladmihalcea.hpjp.spring.data.dto2entity.domain;

import com.vladmihalcea.hpjp.hibernate.association.BidirectionalOneToManyMergeTest;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
@DynamicUpdate
public class Post {

    @Id
    private Long id;

    private String title;

    private int rating;

    @OneToOne(
        mappedBy = "post",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL
    )
    private PostDetails details;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "post_tag",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

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

    public int getRating() {
        return rating;
    }

    public Post setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public PostDetails getDetails() {
        return details;
    }

    public Post setDetails(PostDetails details) {
        if (details == null) {
            if (this.details != null) {
                this.details.setPost(null);
            }
        }
        else {
            details.setPost(this);
        }
        this.details = details;
        return this;
    }

    public List<PostComment> getComments() {
        return comments;
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

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Post addTag(Tag tag) {
        tags.add(tag);
        tag.getPosts().add(this);
        return this;
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getPosts().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post)) return false;
        return id != null && id.equals(((Post) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
