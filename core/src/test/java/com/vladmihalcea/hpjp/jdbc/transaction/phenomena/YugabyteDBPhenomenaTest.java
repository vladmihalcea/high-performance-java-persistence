package com.vladmihalcea.hpjp.jdbc.transaction.phenomena;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.YugabyteDBDataSourceProvider;
import org.junit.jupiter.api.Disabled;

/**
 * YugabyteDBPhenomenaTest - Test to validate YugabyteDB phenomena
 *
 * @author Vlad Mihalcea
 */
@Disabled
public class YugabyteDBPhenomenaTest extends AbstractPhenomenaTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new YugabyteDBDataSourceProvider();
    }
}
