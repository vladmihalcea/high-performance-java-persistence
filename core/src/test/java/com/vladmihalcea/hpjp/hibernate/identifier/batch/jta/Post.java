package com.vladmihalcea.hpjp.hibernate.identifier.batch.jta;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(generator = "post_id_table", strategy = GenerationType.TABLE)
    @TableGenerator(name = "post_id_table", allocationSize = 10)
    private Long id;
}
