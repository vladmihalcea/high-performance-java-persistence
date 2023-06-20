package com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.domain;

import org.jooq.impl.AbstractConverter;

import java.math.BigInteger;

/**
 * @author Vlad Mihalcea
 */
public class LongConverter extends AbstractConverter<BigInteger, Long> {

    public LongConverter() {
        super(BigInteger.class, Long.class);
    }

    @Override
    public Long from(BigInteger databaseObject) {
        return databaseObject != null ? databaseObject.longValue() : null;
    }

    @Override
    public BigInteger to(Long userObject) {
        return userObject != null ? BigInteger.valueOf(userObject) : null;
    }
}
