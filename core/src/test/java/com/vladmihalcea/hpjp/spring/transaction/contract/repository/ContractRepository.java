package com.vladmihalcea.hpjp.spring.transaction.contract.repository;

import com.vladmihalcea.hpjp.spring.transaction.contract.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Repository
@Transactional(readOnly = true)
public interface ContractRepository extends JpaRepository<Contract, String> {

}
