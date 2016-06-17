package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Vlad Mihalcea
 */
public class JsonObjectSqlTypeDescriptor extends AbstractJsonSqlTypeDescriptor {

    public static final JsonObjectSqlTypeDescriptor INSTANCE = new JsonObjectSqlTypeDescriptor();

    @Override
    public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicBinder<X>(javaTypeDescriptor, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                st.setObject(index, javaTypeDescriptor.unwrap(value, ObjectNode.class, options), getSqlType());
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                    throws SQLException {
                st.setObject(name, javaTypeDescriptor.unwrap(value, ObjectNode.class, options), getSqlType());
            }
        };
    }
}
