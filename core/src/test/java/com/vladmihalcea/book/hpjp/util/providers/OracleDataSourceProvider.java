package com.vladmihalcea.book.hpjp.util.providers;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;

import com.vladmihalcea.book.hpjp.util.ReflectionUtils;

/**
 * @author Vlad Mihalcea
 */
public class OracleDataSourceProvider implements DataSourceProvider {
	@Override
	public String hibernateDialect() {
		return "org.hibernate.dialect.Oracle12cDialect";
	}

	@Override
	public DataSource dataSource() {
		try {
			DataSource dataSource = ReflectionUtils.newInstance( "oracle.jdbc.pool.OracleDataSource" );
			ReflectionUtils.invokeSetter( dataSource, "databaseName", "high_performance_java_persistence" );
			ReflectionUtils.invokeSetter( dataSource, "URL", url() );
			ReflectionUtils.invokeSetter( dataSource, "user", "oracle" );
			ReflectionUtils.invokeSetter( dataSource, "password", "admin" );
			return dataSource;
		}
		catch (Exception e) {
			throw new IllegalStateException( e );
		}
	}

	@Override
	public Class<? extends DataSource> dataSourceClassName() {
		try {
			return (Class<? extends DataSource>) Class.forName( "oracle.jdbc.pool.OracleDataSource" );
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException( e );
		}
	}

	@Override
	public Properties dataSourceProperties() {
		Properties properties = new Properties();
		properties.setProperty( "databaseName", "high_performance_java_persistence" );
		properties.setProperty( "URL", url() );
		properties.setProperty( "user", username() );
		properties.setProperty( "password", password() );
		return properties;
	}

	@Override
	public String url() {
		return "jdbc:oracle:thin:@localhost:1521/xe";
		//return "jdbc:oracle:thin:@localhost:1521/orclpdb1";
	}

	@Override
	public String username() {
		return "oracle";
	}

	@Override
	public String password() {
		return "admin";
	}

	@Override
	public Database database() {
		return Database.ORACLE;
	}
}
