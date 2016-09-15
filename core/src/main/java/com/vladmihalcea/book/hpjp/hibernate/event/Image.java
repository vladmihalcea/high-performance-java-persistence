package com.vladmihalcea.book.hpjp.hibernate.event;

import org.hibernate.annotations.LazyGroup;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Image")
@Table(name = "image")
public class Image {

    @Id
    private Long id;

    @Lob
    @Basic( fetch = FetchType.LAZY )
    @LazyGroup( "lazy" )
    private byte[] content;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}