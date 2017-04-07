package com.vladmihalcea.book.hpjp.util.exception;

import javax.persistence.LockTimeoutException;

import org.hibernate.PessimisticLockException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;

/**
 * @author Vlad Mihalcea
 */
public interface ExceptionUtil {

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
	 * Was the given exception caused by a SQL lock timeout?
	 *
	 * @param e exception
	 *
	 * @return is caused by a SQL lock timeout
	 */
	static boolean isSqlLockTimeout(Exception e) {
		if ( LockAcquisitionException.class.isInstance( e )
				|| LockTimeoutException.class.isInstance( e )
				|| PessimisticLockException.class.isInstance( e )
				|| javax.persistence.PessimisticLockException.class.isInstance( e )
			) {
			return true;
		}
		else {
			Throwable rootCause = ExceptionUtil.rootCause( e );
			if ( rootCause != null && (
					rootCause.getMessage().contains( "timeout" ) ||
							rootCause.getMessage().contains( "timed out" ) )
					) {
				return true;
			}
		}
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
		Throwable rootCause = ExceptionUtil.rootCause( e );
		if ( rootCause != null && (
				rootCause.getMessage().toLowerCase().contains( "connection is close" ) ||
				rootCause.getMessage().toLowerCase().contains( "closed connection" )
		) ) {
			return true;
		}
		return false;
	}
}
