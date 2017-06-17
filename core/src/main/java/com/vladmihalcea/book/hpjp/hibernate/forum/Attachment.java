package com.vladmihalcea.book.hpjp.hibernate.forum;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Attachment")
@Table(name = "attachment")
public class Attachment {

    @Id
    @GeneratedValue
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

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}