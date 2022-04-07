package com.vladmihalcea.book.hpjp.hibernate.identifier.batch.jta;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
public class Post {

    @Id
    @GenericGenerator(name = "table", strategy = "enhanced-table", parameters = {
        @org.hibernate.annotations.Parameter(name = "table_name", value = "sequence_table")
    })
    @GeneratedValue(generator = "table", strategy = GenerationType.TABLE)
    private Long id;
}
