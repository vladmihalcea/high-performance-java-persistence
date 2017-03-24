package com.vladmihalcea.book.hpjp.jdbc.transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLSessionAdvisoryLocksTest extends AbstractPostgreSQLAdvisoryLocksTest {

	@Override
	protected void acquireLock(Connection connection, int logIndex, int workerId) {
		try(PreparedStatement statement =
				connection.prepareStatement("select pg_advisory_lock(?)")) {
			statement.setInt( 1, logIndex );
			statement.executeQuery();
		}
		catch (SQLException e) {
			LOGGER.error( "Worker {} failed with this message: {}", workerId, e.getMessage() );
		}
	}

	@Override
	protected void releaseLock(Connection connection, int logIndex, int workerId) {
		try(PreparedStatement statement =
					connection.prepareStatement("select pg_advisory_unlock(?)")) {
			statement.setInt( 1, logIndex );
			statement.executeQuery();
		}
		catch (SQLException e) {
			LOGGER.error( "Worker {} failed with this message: {}", workerId, e.getMessage() );
		}
	}
}
