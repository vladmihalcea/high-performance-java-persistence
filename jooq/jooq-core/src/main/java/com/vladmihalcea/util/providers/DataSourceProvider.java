package com.vladmihalcea.util.providers;

import io.hypersistence.utils.common.ReflectionUtils;
import org.hibernate.dialect.Dialect;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public interface DataSourceProvider {

    String hibernateDialect();

    DataSource dataSource();

    Class<? extends DataSource> dataSourceClassName();

    Properties dataSourceProperties();

    String url();

    String username();

    String password();

    Database database();

    default Class<? extends Dialect> hibernateDialectClass() {
        return ReflectionUtils.getClass(hibernateDialect());
    }
}
