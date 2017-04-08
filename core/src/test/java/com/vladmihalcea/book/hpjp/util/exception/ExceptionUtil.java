package com.vladmihalcea.book.hpjp.util.exception;

import java.sql.SQLTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.LockTimeoutException;

import org.hibernate.PessimisticLockException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;

/**
 * @author Vlad Mihalcea
 */
public interface ExceptionUtil {

	static List<Class<? extends Exception>> LOCK_TIMEOUT_EXCEPTIONS = Arrays.asList(
		LockAcquisitionException.class,
		LockTimeoutException.class,
		PessimisticLockException.class,
		javax.persistence.PessimisticLockException.class,
		SQLTimeoutException.class
	);

	/**
	 * Get the root cause of a particular {@code Throwable}
	 *
	 * @param t exception
	 *
	 * @return exception root cause
	 */
	static Throwable rootCause(Throwable t) {
		Throwable cause = t.getCause();
		if ( cause != null && cause != t ) {
			return rootCause( cause );
		}
		return t;
	}

	/**
	 * Is the given throwable caused by a database lock timeout?
	 *
	 * @param e exception
	 *
	 * @return is caused by a database lock timeout
	 */
	static boolean isLockTimeout(Throwable e) {
		AtomicReference<Throwable> cause = new AtomicReference<>(e);
		do {
			if ( LOCK_TIMEOUT_EXCEPTIONS.stream().anyMatch( c -> c.isInstance( cause.get() ) ) ||
				e.getMessage().contains( "timeout" ) ||
				e.getMessage().contains( "timed out" ) ||
				e.getMessage().contains( "time out" )
			) {
				return true;
			} else {
				cause.set( cause.get().getCause() );
			}
		}
		while ( cause.get().getCause() != null || cause.get().getCause() != cause.get() );
		return false;
	}

	/**
	 * Was the given exception caused by a SQL connection close
	 *
	 * @param e exception
	 *
	 * @return is caused by a SQL connection close
	 */
	static boolean isConnectionClose(Exception e) {
		AtomicReference<Throwable> cause = new AtomicReference<>(e);
		do {
			if ( cause.get().getMessage().toLowerCase().contains( "connection is close" ) ||
				cause.get().getMessage().toLowerCase().contains( "closed connection" )
			) {
				return true;
			} else {
				cause.set( cause.get().getCause() );
			}
		}
		while ( cause.get().getCause() != null || cause.get().getCause() != cause.get() );
		return false;
	}
}
