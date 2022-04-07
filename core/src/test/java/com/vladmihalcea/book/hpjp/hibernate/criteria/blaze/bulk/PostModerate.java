package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.bulk;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
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
