package com.vladmihalcea.hpjp.spring.stateless.domain;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.StandardGenerator;

/**
 * @author Vlad Mihalcea
 */
public class NoOpGenerator implements IdentifierGenerator, StandardGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) {
        return null;
    }
}
