package com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.dto;

/**
 * @author Vlad Mihalcea
 */
public class BookDTO {

    private Long id;

    private String title;

    private long isbn;

    private CategoryDTO category;

    public BookDTO(Long id, String title, Number isbn, Short categoryId) {
        this.id = id;
        this.title = title;
        this.isbn = isbn.longValue();
        this.category = new CategoryDTO(categoryId);
    }

    public Long getId() {
        return id;
    }

    public BookDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public BookDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public long getIsbn() {
        return isbn;
    }

    public BookDTO setIsbn(long isbn) {
        this.isbn = isbn;
        return this;
    }

    public CategoryDTO getCategory() {
        return category;
    }

    public BookDTO setCategory(CategoryDTO category) {
        this.category = category;
        return this;
    }
}
