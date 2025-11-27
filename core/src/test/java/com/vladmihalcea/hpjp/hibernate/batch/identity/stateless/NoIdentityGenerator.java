package com.vladmihalcea.hpjp.hibernate.batch.identity.stateless;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * @author Vlad Mihalcea
 */
public class NoIdentityGenerator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) {
        return null;
    }
}
