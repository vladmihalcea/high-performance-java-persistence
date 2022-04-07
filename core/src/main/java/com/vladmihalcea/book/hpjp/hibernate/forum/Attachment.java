package com.vladmihalcea.book.hpjp.hibernate.forum;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Attachment")
@Table(name = "attachment")
public class Attachment {

    @Id
    private Long id;

    private String name;

    @Enumerated
    @Column(name = "media_type")
    private MediaType mediaType;

    @Lob
    @Basic( fetch = FetchType.LAZY )
    private byte[] content;

    public Long getId() {
        return id;
    }

    public Attachment setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Attachment setName(String name) {
        this.name = name;
        return this;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public Attachment setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public byte[] getContent() {
        return content;
    }

    public Attachment setContent(byte[] content) {
        this.content = content;
        return this;
    }
}