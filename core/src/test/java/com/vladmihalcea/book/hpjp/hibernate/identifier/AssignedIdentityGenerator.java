package com.vladmihalcea.book.hpjp.hibernate.identifier;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;

import java.io.Serializable;

/**
 * AssignedIdentityGenerator - Assigned IdentityGenerator
 *
 * @author Vlad Mihalcea
 */
public class AssignedIdentityGenerator extends IdentityGenerator {

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
