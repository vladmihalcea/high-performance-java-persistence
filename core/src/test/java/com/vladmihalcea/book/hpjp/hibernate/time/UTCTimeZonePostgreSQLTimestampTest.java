package com.vladmihalcea.book.hpjp.hibernate.time;

import org.hibernate.cfg.AvailableSettings;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class UTCTimeZonePostgreSQLTimestampTest extends DefaultPostgreSQLTimestampTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.setProperty(AvailableSettings.JDBC_TIME_ZONE, "UTC");
        return properties;
    }

    @Override
    protected String expectedServerTimestamp() {
        return "2016-08-25 11:23:46";
    }
}
