package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab.cte;

import com.blazebit.persistence.CTE;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

@CTE
@Entity
public class TabKeyVer implements Serializable {
    @Id
    private String tabKey;
    @Id
    private Long tabVer;
}