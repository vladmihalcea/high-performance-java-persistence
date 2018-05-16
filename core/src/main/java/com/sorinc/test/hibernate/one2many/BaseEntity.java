/**
 * check: https://www.callicoder.com/hibernate-spring-boot-jpa-one-to-many-mapping-example/
 */

package com.sorinc.test.hibernate.one2many;

import lombok.AccessLevel;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@MappedSuperclass
public abstract class BaseEntity<BusinessObject> implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Getter(value = AccessLevel.PROTECTED)
    private Long id;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    protected Date createdAt;

    @Version
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    protected Date updatedAt;


    public abstract BusinessObject toBusinessObject();

}
