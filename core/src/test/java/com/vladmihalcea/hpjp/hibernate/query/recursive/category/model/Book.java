package com.vladmihalcea.hpjp.hibernate.query.recursive.category.model;

import com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.dto.BookDTO;
import com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.dto.CategoryDTO;
import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "book")
@SqlResultSetMapping(
    name = "BookCategory",
    classes = {
        @ConstructorResult(
            targetClass = BookDTO.class,
            columns = {
                @ColumnResult(name = "b.id"),
                @ColumnResult(name = "b.title"),
                @ColumnResult(name = "b.isbn"),
                @ColumnResult(name = "b.category_id")
            }
        ),
        @ConstructorResult(
            targetClass = CategoryDTO.class,
            columns = {
                @ColumnResult(name = "c.id"),
                @ColumnResult(name = "c.name"),
                @ColumnResult(name = "c.parent_id")
            }
        )
    }
)
public class Book {

    @Id
    @GeneratedValue
    private Long id;

    @Column(length = 50)
    private String title;

    @Column(columnDefinition = "numeric(13)")
    @NaturalId
    private long isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    public Long getId() {
        return id;
    }

    public Book setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Book setTitle(String title) {
        this.title = title;
        return this;
    }

    public long getIsbn() {
        return isbn;
    }

    public Book setIsbn(long isbn) {
        this.isbn = isbn;
        return this;
    }

    public Category getCategory() {
        return category;
    }

    public Book setCategory(Category category) {
        this.category = category;
        return this;
    }
}
