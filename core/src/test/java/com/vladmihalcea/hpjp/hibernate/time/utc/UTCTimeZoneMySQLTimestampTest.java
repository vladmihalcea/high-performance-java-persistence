package com.vladmihalcea.hpjp.hibernate.time.utc;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;
import org.hibernate.cfg.AvailableSettings;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class UTCTimeZoneMySQLTimestampTest extends DefaultMySQLTimestampTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.setProperty(AvailableSettings.JDBC_TIME_ZONE, "UTC");
        return properties;
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        //http://stackoverflow.com/questions/7605953/how-to-change-mysql-timezone-in-java-connection
        MySQLDataSourceProvider provider = (MySQLDataSourceProvider) super.dataSourceProvider();
        provider.setUseLegacyDatetimeCode(false);
        return provider;
    }

    @Override
    protected String expectedServerTimestamp() {
        return "2016-08-25 11:23:46";
    }
}
