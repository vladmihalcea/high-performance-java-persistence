package com.vladmihalcea.book.hpjp.hibernate.type.json.model;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.vladmihalcea.book.hpjp.hibernate.type.array.EnumArrayType;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.vladmihalcea.book.hpjp.hibernate.type.array.IntArrayType;
import com.vladmihalcea.book.hpjp.hibernate.type.array.StringArrayType;
import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonNodeBinaryType;
import com.vladmihalcea.book.hpjp.hibernate.type.json.JsonStringType;

/**
 * @author Vlad Mihalcea
 */
@TypeDefs({
    @TypeDef(name = "string-array", typeClass = StringArrayType.class),
    @TypeDef(name = "int-array", typeClass = IntArrayType.class),
    @TypeDef(name = "enum-array", typeClass = EnumArrayType.class),
    @TypeDef(name = "json", typeClass = JsonStringType.class),
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class),
    @TypeDef(name = "jsonb-node", typeClass = JsonNodeBinaryType.class),
})
@MappedSuperclass
public class BaseEntity {

    @Id
    private Long id;

    @Version
    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }
}
