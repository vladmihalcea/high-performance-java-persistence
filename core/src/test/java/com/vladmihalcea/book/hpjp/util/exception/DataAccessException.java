package com.vladmihalcea.book.hpjp.util.exception;

/**
 * @author Vlad Mihalcea
 */
public class DataAccessException extends RuntimeException {
	public DataAccessException() {
	}

	public DataAccessException(String message) {
		super( message );
	}

	public DataAccessException(String message, Throwable cause) {
		super( message, cause );
	}

	public DataAccessException(Throwable cause) {
		super( cause );
	}

	public DataAccessException(
			String message,
			Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super( message, cause, enableSuppression, writableStackTrace );
	}
}
