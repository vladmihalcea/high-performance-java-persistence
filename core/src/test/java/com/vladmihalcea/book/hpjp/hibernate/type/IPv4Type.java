package com.vladmihalcea.book.hpjp.hibernate.type;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.net.Inet4Address;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * <code>IPv4Type</code> - IPv4 Type
 *
 * @author Vlad Mihalcea
 */
public class IPv4Type implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.OTHER};
    }

    @Override
    public Class returnedClass() {
        return Inet4Address.class;
    }

    @Override
    public boolean equals(Object x, Object y) {
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names,
                              SessionImplementor session, Object owner) throws SQLException {
        String ip = rs.getString(names[0]);
        if (ip != null) {
            return new IPv4(ip);
        }
        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index,
                            SessionImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            PGobject holder = new PGobject();
            holder.setType("inet");
            holder.setValue(IPv4.class.cast(value).getAddress());
            st.setObject(index, holder);
        }
    }

    @Override
    public Object deepCopy(Object value) {
        if (value == null)
            return null;
        else {
            return new IPv4(IPv4.class.cast(value).getAddress());
        }
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) {
        return Serializable.class.cast(deepCopy(value));
    }

    @Override
    public Object assemble(Serializable cached, Object owner) {
        return deepCopy(cached);
    }

    @Override
    public Object replace(Object original, Object target, Object owner) {
        return deepCopy(original);
    }

}