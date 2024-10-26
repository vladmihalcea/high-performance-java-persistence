package com.vladmihalcea.hpjp.spring.transaction.transfer.repository;

import com.vladmihalcea.hpjp.spring.transaction.transfer.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Repository
@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, String> {

    @Query(value = "SELECT balance FROM account WHERE id = :id", nativeQuery = true)
    long getBalance(@Param("id") String id);

    @Query(value = "UPDATE account SET balance = balance + :amount WHERE id = :id", nativeQuery = true)
    @Modifying
    @Transactional
    int addToBalance(@Param("id") String id, @Param("amount") long cents);
}
