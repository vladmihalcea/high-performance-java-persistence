package com.vladmihalcea.book.hpjp.util.providers;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;

import net.sourceforge.jtds.jdbcx.JtdsDataSource;

/**
 * @author Vlad Mihalcea
 */
public class JTDSDataSourceProvider implements DataSourceProvider {
	@Override
	public String hibernateDialect() {
		return "org.hibernate.dialect.SQLServer2012Dialect";
	}

	@Override
	public DataSource dataSource() {
		JtdsDataSource dataSource = new JtdsDataSource();
		dataSource.setServerName( "localhost" );
		dataSource.setDatabaseName( "high_performance_java_persistence" );
		dataSource.setInstance( "SQLEXPRESS" );
		dataSource.setUser( "sa" );
		dataSource.setPassword( "adm1n" );
		return dataSource;
	}

	@Override
	public Class<? extends DataSource> dataSourceClassName() {
		return JtdsDataSource.class;
	}

	@Override
	public Properties dataSourceProperties() {
		Properties properties = new Properties();
		properties.setProperty( "databaseName", "high_performance_java_persistence" );
		properties.setProperty( "serverName", "localhost" );
		properties.setProperty( "instance", "SQLEXPRESS" );
		properties.setProperty( "user", username() );
		properties.setProperty( "password", password() );
		return properties;
	}

	@Override
	public String url() {
		return null;
	}

	@Override
	public String username() {
		return "sa";
	}

	@Override
	public String password() {
		return "adm1n";
	}

	@Override
	public Database database() {
		return Database.SQLSERVER;
	}
}
