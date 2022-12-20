package com.vladmihalcea.book.hpjp.util.providers;

import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

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
	YUGABYTEDB(YugabyteDBDataSourceProvider.class),
	COCKROACHDB(CockroachDBDataSourceProvider.class),
	;

	private Class<? extends DataSourceProvider> dataSourceProviderClass;

	Database(Class<? extends DataSourceProvider> dataSourceProviderClass) {
		this.dataSourceProviderClass = dataSourceProviderClass;
	}

	public DataSourceProvider dataSourceProvider() {
		return ReflectionUtils.newInstance(dataSourceProviderClass.getName());
	}

	public static Database of(Dialect dialect) {
		Class<? extends Dialect> dialectClass = dialect.getClass();
		for(Database database : values()) {
			if(database.dataSourceProvider().hibernateDialectClass().isAssignableFrom(dialectClass)) {
				return database;
			}
		}
		throw new UnsupportedOperationException(
			String.format(
				"The provided Dialect [%s] is not supported!",
				dialectClass
			)
		);
	}
}
