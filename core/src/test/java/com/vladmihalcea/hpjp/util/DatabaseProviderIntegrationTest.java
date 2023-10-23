package com.vladmihalcea.hpjp.util;

import com.vladmihalcea.hpjp.util.providers.Database;
import org.assertj.core.util.Arrays;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public abstract class DatabaseProviderIntegrationTest extends AbstractTest {

    private final Database database;

    public DatabaseProviderIntegrationTest(Database database) {
        this.database = database;
    }

    @Parameterized.Parameters
    public static Collection<Database[]> databases() {
        List<Database[]> databases = new ArrayList<>();
        databases.add(Arrays.array(Database.ORACLE));
        databases.add(Arrays.array(Database.SQLSERVER));
        databases.add(Arrays.array(Database.POSTGRESQL));
        databases.add(Arrays.array(Database.MYSQL));
        //databases.add(Arrays.array(Database.YUGABYTEDB));
        return databases;
    }

    @Override
    protected Database database() {
        return database;
    }
}
