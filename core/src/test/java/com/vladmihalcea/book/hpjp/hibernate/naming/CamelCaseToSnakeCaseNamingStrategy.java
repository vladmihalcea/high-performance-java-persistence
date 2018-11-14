package com.vladmihalcea.book.hpjp.hibernate.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Vlad Mihalcea
 */
public class CamelCaseToSnakeCaseNamingStrategy extends PhysicalNamingStrategyStandardImpl {

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        return formatIdentifier(super.toPhysicalTableName(name, context));
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        return formatIdentifier(super.toPhysicalColumnName(name, context));
    }

    private Identifier formatIdentifier(Identifier identifier) {
        String name = identifier.getText();

        String formattedName = name.replaceAll("([a-z]+)([A-Z]+)", "$1\\_$2").toLowerCase();

        return !formattedName.equals(name) ?
                Identifier.toIdentifier(formattedName, identifier.isQuoted()) :
                identifier;
    }
}
