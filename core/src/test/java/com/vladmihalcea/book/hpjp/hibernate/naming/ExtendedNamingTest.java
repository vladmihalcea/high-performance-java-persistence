package com.vladmihalcea.book.hpjp.hibernate.naming;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class ExtendedNamingTest extends DefaultNamingTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.physical_naming_strategy", "com.vladmihalcea.book.hpjp.hibernate.naming.OracleNamingStrategy");
        return properties;
    }
}
