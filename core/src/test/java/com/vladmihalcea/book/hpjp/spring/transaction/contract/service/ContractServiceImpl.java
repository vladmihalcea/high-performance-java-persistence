package com.vladmihalcea.book.hpjp.spring.transaction.contract.service;

import com.vladmihalcea.book.hpjp.spring.transaction.contract.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Service
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Transactional
    public boolean transfer(String fromIban, String toIban, long cents) {
        boolean status = true;

        return status;
    }
}
