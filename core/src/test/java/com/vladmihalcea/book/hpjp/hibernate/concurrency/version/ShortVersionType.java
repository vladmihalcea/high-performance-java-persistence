package com.vladmihalcea.book.hpjp.hibernate.concurrency.version;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.ShortType;

/**
 * @author Vlad Mihalcea
 */
public class ShortVersionType extends ShortType {

    @Override
    public Short seed(SharedSessionContractImplementor session) {
        return Short.MIN_VALUE;
    }
}
