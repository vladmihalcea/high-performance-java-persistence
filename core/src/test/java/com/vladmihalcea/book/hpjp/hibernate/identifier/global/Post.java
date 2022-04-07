package com.vladmihalcea.book.hpjp.hibernate.identifier.global;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(generator = "post_sequence", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "post_sequence", allocationSize = 10)
    private Long id;

    private String title;

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
}
