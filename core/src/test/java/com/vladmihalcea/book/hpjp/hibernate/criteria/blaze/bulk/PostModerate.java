package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.bulk;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
public abstract class PostModerate<T extends PostModerate> {

    @Enumerated(EnumType.ORDINAL)
    @Column(columnDefinition = "tinyint")
    private PostStatus status = PostStatus.PENDING;

    @Column(name = "updated_on")
    private Date updatedOn = new Date();

    public PostStatus getStatus() {
        return status;
    }

    public T setStatus(PostStatus status) {
        this.status = status;
        return (T) this;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public T setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
        return (T) this;
    }
}
