package com.vladmihalcea.hpjp.hibernate.index.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Book")
@Table(name = "book")
public class Book {

    @Id
    private Long id;

    @Column(length = 100)
    private String title;

    @Column(name = "published_on")
    private LocalDateTime publishedOn = LocalDateTime.now();

    @Column(name = "author", length = 50)
    private String author;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private String properties;

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

    public LocalDateTime getPublishedOn() {
        return publishedOn;
    }

    public Book setPublishedOn(LocalDateTime publishedOn) {
        this.publishedOn = publishedOn;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Book setAuthor(String author) {
        this.author = author;
        return this;
    }

    public Book setProperties(String properties) {
        this.properties = properties;
        return this;
    }

    public JsonNode getJsonNodeProperties() {
        return JacksonUtil.toJsonNode(properties);
    }
}
