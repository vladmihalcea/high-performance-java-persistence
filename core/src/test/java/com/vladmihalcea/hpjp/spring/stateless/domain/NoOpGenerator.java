package com.vladmihalcea.hpjp.spring.stateless.domain;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.IdentityGenerator;

/**
 * @author Vlad Mihalcea
 */
public class NoOpGenerator extends IdentityGenerator
        implements BeforeExecutionGenerator {
    @Override
    public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
        //necessary so StatelessSessionImpl will try to generate the id before insert, otherwise it throws an error because batch insert doesn't return an id
        return false;
    }

    @Override
    public Object generate(
            SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        return null;
    }

    @Override
    public boolean generatedOnExecution() {
        return true;
    }
}
