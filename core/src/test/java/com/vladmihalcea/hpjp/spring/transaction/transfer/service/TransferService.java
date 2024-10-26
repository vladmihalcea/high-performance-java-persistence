package com.vladmihalcea.hpjp.spring.transaction.transfer.service;

import com.vladmihalcea.hpjp.spring.transaction.transfer.domain.Account;
import com.vladmihalcea.hpjp.spring.transaction.transfer.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
public class TransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void transfer(String sourceAccount, String destinationAccount, long amount) {
        if(accountRepository.getBalance(sourceAccount) >= amount) {
            accountRepository.addToBalance(sourceAccount, (-1) * amount);
            accountRepository.addToBalance(destinationAccount, amount);
        }
    }

    @Transactional
    public void transferOptimisticLocking(String fromIban, String toIban, long cents) {
        Account fromAccount = accountRepository.findById(fromIban).orElse(null);
        Account toAccount = accountRepository.findById(toIban).orElse(null);
        long fromBalance = fromAccount.getBalance();

        if(fromBalance >= cents) {

            fromAccount.setBalance(fromAccount.getBalance() - cents);
            toAccount.setBalance(toAccount.getBalance() + cents);
        }
    }
}
