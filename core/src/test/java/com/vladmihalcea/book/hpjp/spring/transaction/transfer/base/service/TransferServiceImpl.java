package com.vladmihalcea.book.hpjp.spring.transaction.transfer.base.service;

import com.vladmihalcea.book.hpjp.spring.transaction.transfer.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
public class TransferServiceImpl implements TransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean transfer(String fromIban, String toIban, long cents) {
        boolean status = true;

        long fromBalance = accountRepository.getBalance(fromIban);

        if(fromBalance >= cents) {
            status &= accountRepository.addBalance(fromIban, (-1) * cents) > 0;
            status &= accountRepository.addBalance(toIban, cents) > 0;
        }

        return status;
    }
}
