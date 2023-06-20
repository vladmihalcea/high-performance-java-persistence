package com.vladmihalcea.hpjp.hibernate.concurrency.acid;

import com.vladmihalcea.hpjp.util.providers.Database;

/**
 * @author Vlad Mihalcea
 */
public class ACIDRaceConditionRepeatableReadMySQLTest extends ACIDReadModifyWriteRepeatableReadTest {

    @Override
    protected Database database() {
        return Database.MYSQL;
    }
}
