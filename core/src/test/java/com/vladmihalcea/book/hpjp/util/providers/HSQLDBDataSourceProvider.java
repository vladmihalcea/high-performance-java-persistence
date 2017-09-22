package com.vladmihalcea.book.hpjp.util.providers;

import java.util.Properties;
import javax.sql.DataSource;

import org.hsqldb.jdbc.JDBCDataSource;

/**
 * @author Vlad Mihalcea
 */
public class HSQLDBDataSourceProvider implements DataSourceProvider {

	@Override
	public String hibernateDialect() {
		return "org.hibernate.dialect.HSQLDialect";
	}

	@Override
	public DataSource dataSource() {
		JDBCDataSource dataSource = new JDBCDataSource();
		dataSource.setUrl( "jdbc:hsqldb:mem:test" );
		dataSource.setUser( "sa" );
		dataSource.setPassword( "" );
		return dataSource;
	}

	@Override
	public Class<? extends DataSource> dataSourceClassName() {
		return JDBCDataSource.class;
	}

	@Override
	public Properties dataSourceProperties() {
		Properties properties = new Properties();
		properties.setProperty( "url", url() );
		properties.setProperty( "user", username() );
		properties.setProperty( "password", password() );
		return properties;
	}

	@Override
	public String url() {
		return "jdbc:hsqldb:mem:test";
	}

	@Override
	public String username() {
		return "sa";
	}

	@Override
	public String password() {
		return "";
	}

	@Override
	public Database database() {
		return Database.HSQLDB;
	}
}
