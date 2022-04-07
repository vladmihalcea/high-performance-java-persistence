package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.TableGenerator;

import java.io.Serializable;

/**
 * AssignedTableGenerator - Assigned TableGenerator
 *
 * @author Vlad Mihalcea
 */
public class AssignedTableGenerator extends TableGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) {
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
