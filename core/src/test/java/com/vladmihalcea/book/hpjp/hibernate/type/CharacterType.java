package com.vladmihalcea.book.hpjp.hibernate.type;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Vlad Mihalcea
 */
public class CharacterType extends ImmutableType<Character> {

    public CharacterType() {
        super(Character.class);
    }

    @Override
    public int[] sqlTypes() { return new int[]{Types.CHAR}; }

    @Override
    public Character get(ResultSet rs, String[] names,
                    SharedSessionContractImplementor session, Object owner) throws SQLException {
        String value = rs.getString(names[0]);
        return (value != null && value.length() > 0) ? value.charAt(0) : null;
    }

    @Override
    public void set(PreparedStatement st, Character value, int index,
                    SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.CHAR);
        } else {
            st.setString(index, String.valueOf(value));
        }
    }
}