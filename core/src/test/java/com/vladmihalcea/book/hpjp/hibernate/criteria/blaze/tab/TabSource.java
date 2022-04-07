package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "tab_source")
public class TabSource implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tab_key", unique = true, nullable = false, precision = 20)
  private long tabKey;

  @Column(name = "tab_acronym", length = 10)
  private String tabAcronym;

  @OneToMany(mappedBy = "tabSource")
  private List<TabVersion> tabVersions;

  public TabSource() {
  }

  public long getTabKey() {
    return this.tabKey;
  }

  public void setTabKey(long tabKey) {
    this.tabKey = tabKey;
  }

  public String getTabAcronym() {
    return this.tabAcronym;
  }

  public void setTabAcronym(String tabAcronym) {
    this.tabAcronym = tabAcronym;
  }

  public List<TabVersion> getTabVersions() {
    return this.tabVersions;
  }

  public void setTabVersions(List<TabVersion> tabVersions) {
    this.tabVersions = tabVersions;
  }

  public TabVersion addTabVersion(TabVersion tabVersion) {
    getTabVersions().add(tabVersion);
    tabVersion.setTabSource(this);

    return tabVersion;
  }

  public TabVersion removeTabVersion(TabVersion tabVersion) {
    getTabVersions().remove(tabVersion);
    tabVersion.setTabSource(null);

    return tabVersion;
  }

}