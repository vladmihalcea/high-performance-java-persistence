package com.vladmihalcea.hpjp.spring.transaction.transfer.service;

import com.vladmihalcea.hpjp.spring.transaction.transfer.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public class TransferService {

    @Autowired
    private AccountRepository accountRepository;

    //@Transactional
    public boolean transfer(
            String sourceAccount,
            String destinationAccount,
            long amount) {
        boolean status = true;

        if(accountRepository.getBalance(sourceAccount) >= amount) {
            status &= accountRepository.addToBalance(sourceAccount, (-1) * amount) > 0;
            status &= accountRepository.addToBalance(destinationAccount, amount) > 0;
        }

        return status;
    }

    //Using optimistic locking to fix the problem
    /*@Override
    @Transactional
    public boolean transfer(String fromIban, String toIban, long cents) {
        boolean status = true;

        Account fromAccount = accountRepository.findById(fromIban).orElse(null);
        Account toAccount = accountRepository.findById(toIban).orElse(null);
        long fromBalance = fromAccount.getBalance();

        if(fromBalance >= cents) {

            fromAccount.setBalance(fromAccount.getBalance() - cents);
            toAccount.setBalance(toAccount.getBalance() + cents);
        }

        return status;
    }*/
}
