package com.vladmihalcea.util.providers;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import org.hibernate.dialect.SQLServerDialect;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerDataSourceProvider extends AbstractContainerDataSourceProvider {

    private boolean sendStringParametersAsUnicode = false;

    private Boolean useBulkCopyForBatchInsert;

    public boolean isSendStringParametersAsUnicode() {
        return sendStringParametersAsUnicode;
    }

    public SQLServerDataSourceProvider setSendStringParametersAsUnicode(boolean sendStringParametersAsUnicode) {
        this.sendStringParametersAsUnicode = sendStringParametersAsUnicode;
        return this;
    }

    public Boolean getUseBulkCopyForBatchInsert() {
        return useBulkCopyForBatchInsert;
    }

    public SQLServerDataSourceProvider setUseBulkCopyForBatchInsert(Boolean useBulkCopyForBatchInsert) {
        this.useBulkCopyForBatchInsert = useBulkCopyForBatchInsert;
        return this;
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
        dataSource.setUser(username());
        dataSource.setPassword(password());
        if (useBulkCopyForBatchInsert != null) {
            dataSource.setUseBulkCopyForBatchInsert(useBulkCopyForBatchInsert);
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
        properties.setProperty("URL", url());
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
}
