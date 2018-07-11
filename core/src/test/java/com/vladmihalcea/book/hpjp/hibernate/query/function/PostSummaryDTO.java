package com.vladmihalcea.book.hpjp.hibernate.query.function;

/**
 * @author Vlad Mihalcea
 */
public class PostSummaryDTO {

    private Long id;

    private String title;

    private String tags;

    public PostSummaryDTO() {
    }

    public PostSummaryDTO(Long id, String title, String tags) {
        this.id = id;
        this.title = title;
        this.tags = tags;
    }

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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
