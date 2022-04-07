package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "tab_version")
public class TabVersion implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tab_key", unique = true, nullable = false, precision = 20)
  private long tabKey;

  @Column(name = "tab_time_stamp", nullable = false, precision = 10)
  private BigDecimal tabTimeStamp;

  @Column(name = "tab_acronym", nullable = false, length = 120)
  private String tabAcronym;

  @OneToMany(mappedBy = "tabVersion")
  private List<TabInstance> tabInstances;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tab_source", nullable = false)
  private TabSource tabSource;

  public TabVersion() {
  }

  public long getTabKey() {
    return this.tabKey;
  }

  public void setTabKey(long tabKey) {
    this.tabKey = tabKey;
  }

  public BigDecimal getTabTimeStamp() {
    return this.tabTimeStamp;
  }

  public void setTabTimeStamp(BigDecimal tabTimeStamp) {
    this.tabTimeStamp = tabTimeStamp;
  }

  public String getTabAcronym() {
    return this.tabAcronym;
  }

  public void setTabAcronym(String tabAcronym) {
    this.tabAcronym = tabAcronym;
  }

  public List<TabInstance> getTabInstances() {
    return this.tabInstances;
  }

  public void setTabInstances(List<TabInstance> tabInstances) {
    this.tabInstances = tabInstances;
  }

  public TabInstance addTabInstance(TabInstance tabInstance) {
    getTabInstances().add(tabInstance);
    tabInstance.setTabVersion(this);

    return tabInstance;
  }

  public TabInstance removeTabInstance(TabInstance tabInstance) {
    getTabInstances().remove(tabInstance);
    tabInstance.setTabVersion(null);

    return tabInstance;
  }

  public TabSource getTabSource() {
    return this.tabSource;
  }

  public void setTabSource(TabSource tabSource) {
    this.tabSource = tabSource;
  }

}