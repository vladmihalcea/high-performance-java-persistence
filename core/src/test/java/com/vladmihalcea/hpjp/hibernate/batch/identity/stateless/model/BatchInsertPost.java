package com.vladmihalcea.hpjp.hibernate.batch.identity.stateless.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLInsert;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
@SQLInsert(sql = "insert into post (created_by,created_on,title,updated_by,updated_on,version,id) values (?,?,?,?,?,?,default)")
public class BatchInsertPost extends AbstractPost<Post> {

    @Id
    @Column(insertable = false)
    @GeneratedValue(generator = "mysql_identity_generator")
    @GenericGenerator(
        name = "mysql_identity_generator",
        strategy = "com.vladmihalcea.hpjp.hibernate.batch.identity.stateless.NoIdentityGenerator"
    )
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
