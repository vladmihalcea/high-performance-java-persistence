package com.vladmihalcea.hpjp.spring.data.unidirectional.domain;

import jakarta.persistence.*;

import java.util.Date;
import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post_tags")
public class PostTag extends VersionedEntity {

    @EmbeddedId
    private PostTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(
        foreignKey = @ForeignKey(
            name = "FK_post_tag_post_id"
        )
    )
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(
        foreignKey = @ForeignKey(
            name = "FK_post_tag_tag_id"
        )
    )
    private Tag tag;

    @Column(name = "created_on")
    private Date createdOn = new Date();

    public PostTag() {}

    public PostTag(Post post, Tag tag) {
        this.post = post;
        this.tag = tag;
        this.id = new PostTagId(post.getId(), tag.getId());
    }

    public PostTagId getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostTag that = (PostTag) o;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
