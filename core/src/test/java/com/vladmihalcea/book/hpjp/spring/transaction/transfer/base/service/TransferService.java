package com.vladmihalcea.book.hpjp.spring.transaction.transfer.base.service;

import org.springframework.stereotype.Service;

/**
 * @author Vlad Mihalcea
 */
@Service
public interface TransferService {

    boolean transfer(String fromIban, String toIban, long cents);
}
