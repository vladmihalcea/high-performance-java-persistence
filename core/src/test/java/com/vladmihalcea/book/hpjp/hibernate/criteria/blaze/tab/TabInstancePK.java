package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class TabInstancePK implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "tab_key", insertable = false, updatable = false)
  private long tabKey;

  @Column(name = "tab_ver", insertable = false, updatable = false)
  private long tabVer;

  public TabInstancePK() {
  }

  public long getTabKey() {
    return this.tabKey;
  }

  public void setTabKey(long tabKey) {
    this.tabKey = tabKey;
  }

  public long getTabVer() {
    return this.tabVer;
  }

  public void setTabVer(long tabVer) {
    this.tabVer = tabVer;
  }

  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TabInstancePK)) {
      return false;
    }
    TabInstancePK castOther = (TabInstancePK) other;
    return (this.tabKey == castOther.tabKey) && (this.tabVer == castOther.tabVer);
  }

  public int hashCode() {
    final int prime = 31;
    int hash = 17;
    hash = hash * prime + ((int) (this.tabKey ^ (this.tabKey >>> 32)));
    hash = hash * prime + ((int) (this.tabVer ^ (this.tabVer >>> 32)));

    return hash;
  }
}