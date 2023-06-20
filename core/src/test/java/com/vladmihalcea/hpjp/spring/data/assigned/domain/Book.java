package com.vladmihalcea.hpjp.spring.data.assigned.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Book")
@Table(name = "book")
public class Book {

    @Id
    private Long isbn;

    private String title;

    private String author;

    public Long getIsbn() {
        return isbn;
    }

    public Book setIsbn(Long isbn) {
        this.isbn = isbn;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Book setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Book setAuthor(String author) {
        this.author = author;
        return this;
    }
}
