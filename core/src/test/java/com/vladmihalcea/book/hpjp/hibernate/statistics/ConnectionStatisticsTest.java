package com.vladmihalcea.book.hpjp.hibernate.statistics;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.Post;
import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.PostComment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * ConnectionStatisticsTest - Test Hibernate statistics
 *
 * @author Vlad Mihalcea
 */
public class ConnectionStatisticsTest extends AbstractTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testJdbcOneToManyMapping() {
        doInJPA(connection -> {

        });
        doInJPA(connection -> {

        });
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put(AvailableSettings.GENERATE_STATISTICS, "true");
        return properties;
    }
}
