package com.vladmihalcea.hpjp.spring.transaction.contract.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "annex_signature")
public class AnnexSignature extends BaseSignature<AnnexSignature>
    implements RootAware<Contract> {

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Annex annex;

    public Annex getAnnex() {
        return annex;
    }

    public AnnexSignature setAnnex(Annex annex) {
        this.annex = annex;
        return this;
    }

    @Override
    public Contract root() {
        return annex.root();
    }
}
