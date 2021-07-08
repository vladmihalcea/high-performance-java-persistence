package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.tab.cte;

import com.blazebit.persistence.CTE;

import javax.persistence.Entity;
import javax.persistence.Id;

@CTE
@Entity
public class TabKeyVer {
    @Id
    private String tabKey;

    private Long tabVer;
}