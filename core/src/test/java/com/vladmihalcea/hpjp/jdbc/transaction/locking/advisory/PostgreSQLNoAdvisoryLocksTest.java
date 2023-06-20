package com.vladmihalcea.hpjp.jdbc.transaction.locking.advisory;

import java.sql.Connection;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLNoAdvisoryLocksTest extends AbstractPostgreSQLAdvisoryLocksTest {

	@Override
	protected int acquireLock(Connection connection, int logIndex, int workerId) {
		LOGGER.info( "Worker {} writes to log {}", workerId, logIndex );
		return logIndex;
	}

	@Override
	protected void releaseLock(Connection connection, int logIndex, int workerId) {

	}
}
