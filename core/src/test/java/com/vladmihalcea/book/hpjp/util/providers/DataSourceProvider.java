package com.vladmihalcea.book.hpjp.util.providers;

import java.util.Properties;
import javax.sql.DataSource;

/**
 * @author Vlad Mihalcea
 */
public interface DataSourceProvider {

	enum IdentifierStrategy {
		IDENTITY,
		SEQUENCE
	}

	String hibernateDialect();

	DataSource dataSource();

	Class<? extends DataSource> dataSourceClassName();

	Properties dataSourceProperties();

	String url();

	String username();

	String password();

	Database database();
}
