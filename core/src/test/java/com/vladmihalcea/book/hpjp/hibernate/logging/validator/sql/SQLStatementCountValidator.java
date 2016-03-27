/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql;

import com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql.exception.SQLStatementCountMismatchException;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;

/**
 * SQLStatementCountValidator - Validates recorded statements count.
 *
 * First you execute some operations against your database and then you check how many statements were executed.
 * This is a useful tool against the "N+1" problem or suboptimal DML statements.
 *
 * @author Vlad Mihalcea
 */
public class SQLStatementCountValidator {

    private SQLStatementCountValidator() {}

    /**
     * Reset the statement recorder
     */
    public static void reset() {
        QueryCountHolder.clear();
    }

    /**
     * Assert select statement count
     *
     * @param expectedSelectCount expected select statement count
     */
    public static void assertSelectCount(int expectedSelectCount) {
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        int recordedSelectCount = queryCount.getSelect();
        if (expectedSelectCount != recordedSelectCount) {
            throw new SQLStatementCountMismatchException(expectedSelectCount, recordedSelectCount);
        }
    }

    /**
     * Assert insert statement count
     *
     * @param expectedInsertCount expected insert statement count
     */
    public static void assertInsertCount(int expectedInsertCount) {
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        int recordedInsertCount = queryCount.getInsert();
        if (expectedInsertCount != recordedInsertCount) {
            throw new SQLStatementCountMismatchException(expectedInsertCount, recordedInsertCount);
        }
    }

    /**
     * Assert update statement count
     *
     * @param expectedUpdateCount expected update statement count
     */
    public static void assertUpdateCount(int expectedUpdateCount) {
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        int recordedUpdateCount = queryCount.getUpdate();
        if (expectedUpdateCount != recordedUpdateCount) {
            throw new SQLStatementCountMismatchException(expectedUpdateCount, recordedUpdateCount);
        }
    }

    /**
     * Assert delete statement count
     *
     * @param expectedDeleteCount expected delete statement count
     */
    public static void assertDeleteCount(int expectedDeleteCount) {
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        int recordedDeleteCount = queryCount.getDelete();
        if (expectedDeleteCount != recordedDeleteCount) {
            throw new SQLStatementCountMismatchException(expectedDeleteCount, recordedDeleteCount);
        }
    }
}
