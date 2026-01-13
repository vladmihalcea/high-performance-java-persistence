package com.vladmihalcea.hpjp.spring.stateless.domain;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * @author Vlad Mihalcea
 */
public class NoOpGenerator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) {
        return null;
    }
}
