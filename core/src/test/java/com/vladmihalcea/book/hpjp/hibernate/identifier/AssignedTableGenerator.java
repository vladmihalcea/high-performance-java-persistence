package com.vladmihalcea.book.hpjp.hibernate.identifier;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.enhanced.TableGenerator;

import java.io.Serializable;

/**
 * AssignedTableGenerator - Assigned TableGenerator
 *
 * @author Vlad Mihalcea
 */
public class AssignedTableGenerator extends TableGenerator {

    @Override
    public Serializable generate(SessionImplementor session, Object obj) {
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
