package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab;

import org.springframework.lang.Nullable;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
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
