package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tab_instance")
public class TabInstance extends AbstractEntity<TabInstancePK> {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private TabInstancePK id;

  @Column(name = "tab_acronym", nullable = false, length = 50)
  private String tabAcronym;

  @Column(name = "tab_additional_data", nullable = false, length = 50)
  private String tabAdditionalData;

  @Column(name = "tab_additional_data_number", nullable = false, precision = 10)
  private BigDecimal tabAdditionalDataNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tab_key", nullable = false, insertable = false, updatable = false)
  private TabObject tabObject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tab_ver", nullable = false, insertable = false, updatable = false)
  private TabVersion tabVersion;

  public TabInstance() {
  }

  public TabInstancePK getId() {
    return this.id;
  }

  public void setId(TabInstancePK id) {
    this.id = id;
  }

  public String getTabAcronym() {
    return tabAcronym;
  }

  public void setTabAcronym(String tabAcronym) {
    this.tabAcronym = tabAcronym;
  }

  public String getTabAdditionalData() {
    return tabAdditionalData;
  }

  public void setTabAdditionalData(String tabAdditionalData) {
    this.tabAdditionalData = tabAdditionalData;
  }

  public BigDecimal getTabAdditionalDataNumber() {
    return tabAdditionalDataNumber;
  }

  public void setTabAdditionalDataNumber(BigDecimal tabAdditionalDataNumber) {
    this.tabAdditionalDataNumber = tabAdditionalDataNumber;
  }

  public TabObject getTabObject() {
    return tabObject;
  }

  public void setTabObject(TabObject tabObject) {
    this.tabObject = tabObject;
  }

  public TabVersion getTabVersion() {
    return tabVersion;
  }

  public void setTabVersion(TabVersion tabVersion) {
    this.tabVersion = tabVersion;
  }
}