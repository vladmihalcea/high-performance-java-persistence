package com.vladmihalcea.hpjp.spring.transaction.contract.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "contract_signature")
public class ContractSignature extends BaseSignature<ContractSignature>
    implements RootAware<Contract> {

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Contract contract;

    public Contract getContract() {
        return contract;
    }

    public ContractSignature setContract(Contract contract) {
        this.contract = contract;
        return this;
    }

    @Override
    public Contract root() {
        return contract;
    }
}
