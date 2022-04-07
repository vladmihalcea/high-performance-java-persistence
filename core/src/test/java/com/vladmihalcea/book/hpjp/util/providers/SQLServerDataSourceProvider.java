package com.vladmihalcea.book.hpjp.util.providers;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.book.hpjp.util.providers.queries.Queries;
import com.vladmihalcea.book.hpjp.util.providers.queries.SQLServerQueries;
import org.hibernate.dialect.SQLServerDialect;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerDataSourceProvider implements DataSourceProvider {

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
	public DataSource dataSource() {
		SQLServerDataSource dataSource = new SQLServerDataSource();
		String url = "jdbc:sqlserver://localhost;instance=SQLEXPRESS;databaseName=high_performance_java_persistence;encrypt=true;trustServerCertificate=true;";
		url += "sendStringParametersAsUnicode=" + sendStringParametersAsUnicode;
		dataSource.setURL(url);
		dataSource.setUser("sa");
		dataSource.setPassword("adm1n");
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
	public String url() {
		return "jdbc:sqlserver://localhost;instance=SQLEXPRESS;databaseName=high_performance_java_persistence;user=sa;password=adm1n";
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
