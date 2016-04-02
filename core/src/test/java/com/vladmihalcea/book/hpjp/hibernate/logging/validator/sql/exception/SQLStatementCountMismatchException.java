package com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql.exception;

/**
 * SQLStatementCountMismatchException - Thrown whenever there is a mismatch between expected statements count and
 * the ones being executed.
 *
 * @author Vlad Mihalcea
 */
public class SQLStatementCountMismatchException extends RuntimeException {

    private final int expected;
    private final int recorded;

    public SQLStatementCountMismatchException(int expected, int recorded) {
        super(String.format("Expected %d statement(s) but recorded %d instead!",
            expected, recorded));
        this.expected = expected;
        this.recorded = recorded;
    }

    public int getExpected() {
        return expected;
    }

    public int getRecorded() {
        return recorded;
    }
}
