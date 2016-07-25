package com.vladmihalcea.book.hpjp.hibernate.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Vlad Mihalcea
 */
public class OracleNamingStrategy extends PhysicalNamingStrategyStandardImpl {

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        Identifier original = super.toPhysicalColumnName(name, context);
        if(original.getText().length() > 30) {
            return Identifier.toIdentifier(original.getText().substring(0, 30), original.isQuoted());
        }
        return original;
    }
}
