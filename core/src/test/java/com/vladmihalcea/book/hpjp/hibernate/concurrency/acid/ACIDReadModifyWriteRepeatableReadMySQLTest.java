package com.vladmihalcea.book.hpjp.hibernate.concurrency.acid;

import com.vladmihalcea.book.hpjp.util.providers.Database;

/**
 * @author Vlad Mihalcea
 */
public class ACIDReadModifyWriteRepeatableReadMySQLTest extends ACIDReadModifyWriteRepeatableReadTest {

    @Override
    protected Database database() {
        return Database.MYSQL;
    }
}
