package com.vladmihalcea.book.hpjp.jdbc.transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLNoAdvisoryLocksTest extends AbstractPostgreSQLAdvisoryLocksTest {

	@Override
	protected void acquireLock(Connection connection, int logIndex, int workerId) {

	}

	@Override
	protected void releaseLock(Connection connection, int logIndex, int workerId) {

	}
}
