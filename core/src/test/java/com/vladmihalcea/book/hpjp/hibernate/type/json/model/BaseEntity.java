package com.vladmihalcea.book.hpjp.hibernate.type.json.model;

import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonStringType;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Vlad Mihalcea
 */
@TypeDefs({
    @TypeDef(name = "json", typeClass = JsonStringType.class),
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@MappedSuperclass
public class BaseEntity {

    @Id
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
