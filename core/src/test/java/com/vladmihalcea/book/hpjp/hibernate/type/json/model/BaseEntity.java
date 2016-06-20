package com.vladmihalcea.book.hpjp.hibernate.type.json.model;

import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonStringType;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

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

}
