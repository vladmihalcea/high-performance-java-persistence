package com.vladmihalcea.hpjp.hibernate.inheritance;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;
import org.hibernate.dialect.MySQL8Dialect;

/**
 * @author Vlad Mihalcea
 */
public class TablePerClassMySQLUnionTest extends TablePerClassTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return MySQLUnionSupportDialect.class.getName();
            }
        };
    }

    public static class MySQLUnionSupportDialect extends MySQL8Dialect {
        @Override
        public boolean supportsUnionAll() {
            return false;
        }
    }
}
