package com.vladmihalcea.book.hpjp.hibernate.identifier.global;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(generator = "sequence", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "sequence", allocationSize = 10)
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
