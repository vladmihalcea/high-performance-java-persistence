package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab;

import org.springframework.lang.Nullable;

import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import java.io.Serializable;

@MappedSuperclass
public abstract class AbstractEntity<I> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Transient
  private boolean isNew = true;

  public boolean isNew() {
    return isNew;
  }

  @Nullable
  public abstract I getId();

  @PrePersist
  @PostLoad
  void markNotNew() {
    this.isNew = false;
  }
}
