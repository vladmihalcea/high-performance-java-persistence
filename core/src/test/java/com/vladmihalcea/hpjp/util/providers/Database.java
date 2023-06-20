package com.vladmihalcea.hpjp.util.providers;

import com.vladmihalcea.hpjp.util.ReflectionUtils;
import org.hibernate.dialect.Dialect;
import org.testcontainers.containers.*;

import java.util.Collections;

/**
 * @author Vlad Mihalcea
 */
public enum Database {
	//Mandatory databases
	HSQLDB {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return HSQLDBDataSourceProvider.class;
		}
	},
	POSTGRESQL {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return PostgreSQLDataSourceProvider.class;
		}

		@Override
		protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
			return new PostgreSQLContainer("postgres:15.3");
		}
	},
	ORACLE {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return OracleDataSourceProvider.class;
		}

		@Override
		protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
			return new OracleContainer("gvenzl/oracle-xe:21.3.0-slim");
		}

		@Override
		protected boolean supportsDatabaseName() {
			return false;
		}
	},
	MYSQL {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return MySQLDataSourceProvider.class;
		}

		@Override
		protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
			return new MySQLContainer("mysql:8.0");
		}
	},
	SQLSERVER {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return SQLServerDataSourceProvider.class;
		}

		@Override
		protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
			return new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2019-latest");
		}

		@Override
		protected boolean supportsDatabaseName() {
			return false;
		}

		@Override
		protected boolean supportsCredentials() {
			return false;
		}
	},
	MARIADB {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return MariaDBDataSourceProvider.class;
		}

		@Override
		protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
			return new MariaDBContainer("mariadb:10.10");
		}
	},
	YUGABYTEDB {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return YugabyteDBDataSourceProvider.class;
		}

		@Override
		protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
			return new YugabyteDBYSQLContainer("yugabytedb/yugabyte:2.14.4.0-b26");
		}
	},
	COCKROACHDB {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return CockroachDBDataSourceProvider.class;
		}

		@Override
		protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
			return new CockroachContainer("cockroachdb/cockroach:v22.2.10");
		}

		protected String databaseName() {
			return "high_performance_java_persistence";
		}
	},
	//These databases require manual setup
	YUGABYTEDB_CLUSTER {
		@Override
		public Class<? extends DataSourceProvider> dataSourceProviderClass() {
			return YugabyteDBClusterDataSourceProvider.class;
		}
	},
	;

	private JdbcDatabaseContainer container;

	public JdbcDatabaseContainer getContainer() {
		return container;
	}

	public DataSourceProvider dataSourceProvider() {
		return ReflectionUtils.newInstance(dataSourceProviderClass().getName());
	}
	
	public abstract Class<? extends DataSourceProvider> dataSourceProviderClass();

	public void initContainer(String username, String password) {
		container = (JdbcDatabaseContainer) newJdbcDatabaseContainer()
			.withReuse(true)
			.withEnv(Collections.singletonMap("ACCEPT_EULA", "Y"))
			.withTmpFs(Collections.singletonMap("/testtmpfs", "rw"));
		if(supportsDatabaseName()) {
			container.withDatabaseName(databaseName());
		}
		if(supportsCredentials()) {
			container.withUsername(username).withPassword(password);
		}
		container.start();
	}

	protected JdbcDatabaseContainer newJdbcDatabaseContainer() {
		throw new UnsupportedOperationException(
			String.format(
				"The [%s] database was not configured to use Testcontainers!",
				name()
			)
		);
	}

	protected boolean supportsDatabaseName() {
		return true;
	}

	protected String databaseName() {
		return "high-performance-java-persistence";
	}

	protected boolean supportsCredentials() {
		return true;
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
