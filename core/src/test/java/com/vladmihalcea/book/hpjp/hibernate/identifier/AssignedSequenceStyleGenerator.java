package com.vladmihalcea.book.hpjp.hibernate.identifier;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import java.io.Serializable;

/**
 * AssignedSequenceStyleGenerator - Assigned SequenceStyleGenerator
 *
 * @author Vlad Mihalcea
 */
public class AssignedSequenceStyleGenerator extends SequenceStyleGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) {
        if(obj instanceof Identifiable) {
            Identifiable identifiable = (Identifiable) obj;
            Serializable id = identifiable.getId();
            if(id != null) {
                return id;
            }
        }
        return super.generate(session, obj);
    }
}
