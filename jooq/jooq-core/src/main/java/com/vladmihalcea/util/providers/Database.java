package com.vladmihalcea.util.providers;

import io.hypersistence.utils.common.ReflectionUtils;
import org.hibernate.dialect.Dialect;

/**
 * @author Vlad Mihalcea
 */
public enum Database {
    //Mandatory databases
    POSTGRESQL {
        @Override
        public Class<? extends DataSourceProvider> dataSourceProviderClass() {
            return PostgreSQLDataSourceProvider.class;
        }
    },
    ORACLE {
        @Override
        public Class<? extends DataSourceProvider> dataSourceProviderClass() {
            return OracleDataSourceProvider.class;
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
    },
    SQLSERVER {
        @Override
        public Class<? extends DataSourceProvider> dataSourceProviderClass() {
            return SQLServerDataSourceProvider.class;
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
    ;

    public DataSourceProvider dataSourceProvider() {
        return ReflectionUtils.newInstance(dataSourceProviderClass().getName());
    }

    public abstract Class<? extends DataSourceProvider> dataSourceProviderClass();

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
        for (Database database : values()) {
            if (database.dataSourceProvider().hibernateDialectClass().isAssignableFrom(dialectClass)) {
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
