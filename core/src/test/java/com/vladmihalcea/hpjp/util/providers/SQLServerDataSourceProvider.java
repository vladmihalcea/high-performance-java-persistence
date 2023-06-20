package com.vladmihalcea.hpjp.util.providers;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.hpjp.util.providers.queries.Queries;
import com.vladmihalcea.hpjp.util.providers.queries.SQLServerQueries;
import org.hibernate.dialect.SQLServerDialect;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerDataSourceProvider extends AbstractContainerDataSourceProvider {

	private boolean sendStringParametersAsUnicode = false;

	public boolean isSendStringParametersAsUnicode() {
		return sendStringParametersAsUnicode;
	}

	public void setSendStringParametersAsUnicode(boolean sendStringParametersAsUnicode) {
		this.sendStringParametersAsUnicode = sendStringParametersAsUnicode;
	}

	@Override
	public String hibernateDialect() {
		return SQLServerDialect.class.getName();
	}

	@Override
	public String defaultJdbcUrl() {
		return "jdbc:sqlserver://localhost;instance=SQLEXPRESS;databaseName=high_performance_java_persistence;encrypt=true;trustServerCertificate=true";
	}

	@Override
	public DataSource newDataSource() {
		SQLServerDataSource dataSource = new SQLServerDataSource();
		dataSource.setURL(url());
		JdbcDatabaseContainer container = database().getContainer();
		if(container == null) {
			dataSource.setUser(username());
			dataSource.setPassword(password());
		} else {
			dataSource.setUser(container.getUsername());
			dataSource.setPassword(container.getPassword());
		}
		dataSource.setSendStringParametersAsUnicode(sendStringParametersAsUnicode);
		return dataSource;
	}

	@Override
	public Class<? extends DataSource> dataSourceClassName() {
		return SQLServerDataSource.class;
	}

	@Override
	public Properties dataSourceProperties() {
		Properties properties = new Properties();
		properties.setProperty( "URL", url() );
		return properties;
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


	@Override
	public Queries queries() {
		return SQLServerQueries.INSTANCE;
	}
}
