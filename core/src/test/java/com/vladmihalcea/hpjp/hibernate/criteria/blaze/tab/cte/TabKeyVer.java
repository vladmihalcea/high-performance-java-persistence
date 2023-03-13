package com.vladmihalcea.hpjp.hibernate.criteria.blaze.tab.cte;

import com.blazebit.persistence.CTE;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

@CTE
@Entity
public class TabKeyVer implements Serializable {
    @Id
    private Long tabKey;
    @Id
    private Long tabVer;
}