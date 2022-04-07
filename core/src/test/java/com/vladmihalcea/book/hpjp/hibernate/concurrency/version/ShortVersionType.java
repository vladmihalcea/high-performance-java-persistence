package com.vladmihalcea.book.hpjp.hibernate.concurrency.version;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.ShortJavaType;

/**
 * @author Vlad Mihalcea
 */
public class ShortVersionType extends ShortJavaType {

    @Override
    public Short seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
        return Short.MIN_VALUE;
    }
}
