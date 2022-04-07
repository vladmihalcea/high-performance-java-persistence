package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "tab_object")
public class TabObject extends AbstractEntity<Long> {

  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "tab_key", unique = true, nullable = false, precision = 20)
  private long tabKey;

  @Column(name = "tab_acronym", nullable = false, length = 5)
  private String tabAcronym;

  @OneToMany(mappedBy = "tabObject")
  private List<TabInstance> tabInstances;

  public TabObject() {
  }

  @Override
  public Long getId() {
    return tabKey;
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

  public List<TabInstance> getBasaBetriebsstelles() {
    return this.tabInstances;
  }

  public void setBasaBetriebsstelles(List<TabInstance> tabInstances) {
    this.tabInstances = tabInstances;
  }

  public TabInstance addBasaBetriebsstelle(TabInstance tabInstance) {
    getBasaBetriebsstelles().add(tabInstance);
    tabInstance.setTabObject(this);

    return tabInstance;
  }

  public TabInstance removeBasaBetriebsstelle(TabInstance tabInstance) {
    getBasaBetriebsstelles().remove(tabInstance);
    tabInstance.setTabObject(null);

    return tabInstance;
  }

}