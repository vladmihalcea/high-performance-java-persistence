package com.vladmihalcea.book.hpjp.util.providers;

import com.vladmihalcea.book.hpjp.util.ReflectionUtils;

/**
 * @author Vlad Mihalcea
 */
public enum Database {
	HSQLDB(HSQLDBDataSourceProvider.class),
	POSTGRESQL(PostgreSQLDataSourceProvider.class),
	ORACLE(OracleDataSourceProvider.class),
	MYSQL(MySQLDataSourceProvider.class),
	MARIADB(MariaDBDataSourceProvider.class),
	SQLSERVER(SQLServerDataSourceProvider.class),
	COCKROACHDB(CockroachDBDataSourceProvider.class);

	private Class<? extends DataSourceProvider> dataSourceProviderClass;

	Database(Class<? extends DataSourceProvider> dataSourceProviderClass) {
		this.dataSourceProviderClass = dataSourceProviderClass;
	}

	public DataSourceProvider dataSourceProvider() {
		return ReflectionUtils.newInstance(dataSourceProviderClass.getName());
	}
}
