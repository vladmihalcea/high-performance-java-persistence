package com.vladmihalcea.hpjp.spring.data.dto2entity.dto;

import java.util.List;
import java.util.Set;

/**
 * @author Vlad Mihalcea
 */
public class PostDTO {
    private Long id;

    private String title;

    private PostDetailsDTO details;

    private List<PostCommentDTO> comments;

    private Set<TagDTO> tags;

    public Long getId() {
        return id;
    }

    public PostDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public PostDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public PostDetailsDTO getDetails() {
        return details;
    }

    public PostDTO setDetails(PostDetailsDTO details) {
        this.details = details;
        return this;
    }

    public List<PostCommentDTO> getComments() {
        return comments;
    }

    public PostDTO setComments(List<PostCommentDTO> comments) {
        this.comments = comments;
        return this;
    }

    public Set<TagDTO> getTags() {
        return tags;
    }

    public PostDTO setTags(Set<TagDTO> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostDTO)) return false;
        return id != null && id.equals(((PostDTO) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
