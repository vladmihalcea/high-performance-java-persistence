package com.vladmihalcea.hpjp.hibernate.identifier;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import java.io.Serializable;

/**
 * @author Vlad Mihalcea
 */
public class AssignedSequenceStyleGenerator extends SequenceStyleGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) {
        if(obj instanceof Identifiable identifiable) {
            Serializable id = identifiable.getId();
            if(id != null) {
                return id;
            }
        }
        return super.generate(session, obj);
    }
}
