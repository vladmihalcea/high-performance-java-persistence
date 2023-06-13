package com.vladmihalcea.book.hpjp.jooq.oracle.fetching.multiset.domain;

import org.jooq.impl.EnumConverter;

/**
 * @author Vlad Mihalcea
 */
public class VoteTypeConverter extends EnumConverter<Short, VoteType> {

    public VoteTypeConverter() {
        super(Short.class, VoteType.class);
    }
}
