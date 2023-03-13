package com.vladmihalcea.hpjp.hibernate.identifier;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.Binder;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;

import java.io.Serializable;
import java.sql.PreparedStatement;

/**
 * AssignedIdentityGenerator - Assigned IdentityGenerator
 *
 * @author Vlad Mihalcea
 */
public class AssignedIdentityGenerator extends IdentityGenerator {

    @Override
    public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(PostInsertIdentityPersister persister) {
        InsertGeneratedIdentifierDelegate delegate = super.getGeneratedIdentifierDelegate(persister);
        return new InsertGeneratedIdentifierDelegate() {
            @Override
            public TableInsertBuilder createTableInsertBuilder(BasicEntityIdentifierMapping identifierMapping, Expectation expectation, SessionFactoryImplementor sessionFactory) {
                return delegate.createTableInsertBuilder(identifierMapping, expectation, sessionFactory);
            }

            @Override
            public PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session) {
                return delegate.prepareStatement(insertSql, session);
            }

            @Override
            public Object performInsert(PreparedStatementDetails insertStatementDetails, JdbcValueBindings valueBindings, Object entity, SharedSessionContractImplementor session) {
                Object id = getAssignedIdentifier(entity);
                return id != null ? id : delegate.performInsert(insertStatementDetails, valueBindings, entity, session);
            }

            @Override
            public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
                return delegate.prepareIdentifierGeneratingInsert(context);
            }

            @Override
            public Object performInsert(String insertSQL, SharedSessionContractImplementor session, Binder binder) {
                return delegate.performInsert(insertSQL, session, binder);
            }

            public Object getAssignedIdentifier(Object entity) {
                if(entity instanceof Identifiable identifiable) {
                    return identifiable.getId();
                }
                return null;
            }
        };
    }
}
