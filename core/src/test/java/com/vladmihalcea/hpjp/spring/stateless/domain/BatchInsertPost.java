package com.vladmihalcea.hpjp.spring.stateless.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLInsert;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post")
@SQLInsert(sql = """
INSERT INTO post (
    created_by, created_on, title,
    updated_by, updated_on, version
)
VALUES (
    ?, ?, ?,
    ?, ?, ?
)
""")
public class BatchInsertPost extends AbstractPost<BatchInsertPost> {

    @Id
    @Column(insertable = false)
    @GeneratedValue(generator = "noop_generator")
    @GenericGenerator(
        name = "noop_generator",
        strategy = "com.vladmihalcea.hpjp.spring.stateless.domain.NoOpGenerator"
    )
    private Long id;

    public Long getId() {
        return id;
    }

    public BatchInsertPost setId(Long id) {
        this.id = id;
        return this;
    }

    public static BatchInsertPost valueOf(Post post) {
        return new BatchInsertPost()
            .setId(post.getId())
            .setTitle(post.getTitle())
            .setCreatedBy(post.getCreatedBy())
            .setCreatedOn(post.getCreatedOn())
            .setUpdatedBy(post.getUpdatedBy())
            .setUpdatedOn(post.getUpdatedOn())
            .setVersion(post.getVersion());
    }
}
