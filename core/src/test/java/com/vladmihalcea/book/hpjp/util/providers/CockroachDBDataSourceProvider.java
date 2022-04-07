package com.vladmihalcea.book.hpjp.util.providers;

import com.vladmihalcea.book.hpjp.util.providers.queries.PostgreSQLQueries;
import com.vladmihalcea.book.hpjp.util.providers.queries.Queries;
import org.hibernate.dialect.identity.CockroachDB1920IdentityColumnSupport;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class CockroachDBDataSourceProvider
		implements DataSourceProvider {

	@Override
	public String hibernateDialect() {
		return CockroachDB1920IdentityColumnSupport.class.getName();
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

	@Override
	public Queries queries() {
		return PostgreSQLQueries.INSTANCE;
	}
}
