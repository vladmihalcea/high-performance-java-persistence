package com.vladmihalcea.hpjp.hibernate.batch.identity.stateless;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.StandardGenerator;

/**
 * @author Vlad Mihalcea
 */
public class NoIdentityGenerator implements IdentifierGenerator, StandardGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) {
        return null;
    }
}
