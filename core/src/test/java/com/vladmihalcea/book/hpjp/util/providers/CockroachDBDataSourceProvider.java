package com.vladmihalcea.book.hpjp.util.providers;

import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.PostgreSQL94Dialect;

import com.vladmihalcea.book.hpjp.util.CockroachDBDialect;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * @author Vlad Mihalcea
 */
public class CockroachDBDataSourceProvider
		implements DataSourceProvider {

	@Override
	public String hibernateDialect() {
		return CockroachDBDialect.class.getName();
	}

	@Override
	public DataSource dataSource() {
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setDatabaseName( "high_performance_java_persistence" );
		dataSource.setServerName( host() );
		dataSource.setPortNumber( port() );
		dataSource.setUser( username() );
		dataSource.setPassword( password() );
		dataSource.setSsl( false );
		return dataSource;
	}

	@Override
	public Class<? extends DataSource> dataSourceClassName() {
		return PGSimpleDataSource.class;
	}

	@Override
	public Properties dataSourceProperties() {
		Properties properties = new Properties();
		properties.setProperty( "databaseName", "high_performance_java_persistence" );
		properties.setProperty( "serverName", host() );
		properties.setProperty( "portNumber", String.valueOf( port() ) );
		properties.setProperty( "user", username() );
		properties.setProperty( "password", password() );
		properties.setProperty( "sslmode", "disabled" );
		return properties;
	}

	@Override
	public String url() {
		return String.format(
			"jdbc:postgresql://%s:%d/high_performance_java_persistence?sslmode=disable",
			host(),
			port()
		);
	}

	public String host() {
		return "127.0.0.1";
	}

	public int port() {
		return 26257;
	}

	@Override
	public String username() {
		return "cockroach";
	}

	@Override
	public String password() {
		return "admin";
	}

	@Override
	public Database database() {
		return Database.COCKROACHDB;
	}
}
