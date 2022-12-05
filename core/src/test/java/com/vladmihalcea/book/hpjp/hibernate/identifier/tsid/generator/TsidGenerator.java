package com.vladmihalcea.book.hpjp.hibernate.identifier.tsid.generator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;

import java.lang.reflect.Member;

/**
 * @author Vlad Mihalcea
 */
public class TsidGenerator implements IdentifierGenerator {

    private final Tsid.Generator generator;

    public TsidGenerator(
        Tsid config,
        Member idMember,
        CustomIdGeneratorCreationContext creationContext) {
        generator = config.generator();
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        return generator.random().toLong();
    }
}
