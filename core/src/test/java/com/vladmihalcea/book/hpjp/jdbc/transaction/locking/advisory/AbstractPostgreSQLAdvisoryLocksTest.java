package com.vladmihalcea.book.hpjp.jdbc.transaction.locking.advisory;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractPostgreSQLAdvisoryLocksTest extends AbstractPostgreSQLIntegrationTest {

	private static final String TEMP_FOLDER = System.getProperty( "java.io.tmpdir" );

	private static final int workerCount = 30;

	private static final int logCount = 6;

	private static final int workerLineCount = 10;

	private final CountDownLatch startLatch = new CountDownLatch( 1 );

	private final CountDownLatch endLatch = new CountDownLatch( workerCount );

	private final Executor workers = Executors.newFixedThreadPool( workerCount );

	private static final Path[] logs = new Path[logCount];

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
		};
	}

	@Override
	public void init() {
		super.init();
		try {
			for ( int i = 0; i < logCount; i++ ) {
				Path log = Paths.get( TEMP_FOLDER, String.format( "tx%d.log", i ) );
				Files.deleteIfExists( log );
				Files.createFile( log );
				logs[i] = log;
			}
		}
		catch (IOException e) {
			fail( e.getMessage() );
		}
	}

	@Override
	public void destroy() {
		int lineCount = 0;
		for ( int i = 0; i < logCount; i++ ) {
			try {
				Path log = logs[i];
				lineCount += Files.readAllLines( log ).size();
				Files.deleteIfExists( log );
			}
			catch (IOException e) {
				fail( e.getMessage() );
			}
		}
		super.destroy();

		assertEquals( workerCount * workerLineCount, lineCount );
	}

	@Test
	public void test() {
		long startNanos = System.nanoTime();
		AtomicInteger counter = new AtomicInteger( workerCount );
		for ( int i = 0; i < workerCount; i++ ) {
			final int workerId = i;
			workers.execute( () -> {
				LOGGER.info( "Worker {} is ready", workerId );

				try {
					doInJDBC( connection -> {
						awaitOnLatch( startLatch );
						Integer logIndex = null;

						try {
							logIndex = acquireLock(connection, randomLogIndex(), workerId);
							write( logs[logIndex], workerId );
						}
						finally {
							if ( logIndex != null ) {
								releaseLock(connection, logIndex, workerId);
							}
						}
					} );
				}
				finally {
					LOGGER.info( "Count down {}", counter.decrementAndGet() );
					endLatch.countDown();
				}
			} );
		}
		LOGGER.info( "Start workers" );
		startLatch.countDown();
		awaitOnLatch( endLatch );
		LOGGER.info( "Workers are done in {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
	}

	protected int randomLogIndex() {
		return (int) ( ( Math.random() * 10 ) % logCount );
	}

	protected abstract int acquireLock(Connection connection, int logIndex, int workerId);

	protected abstract void releaseLock(Connection connection, int logIndex, int workerId);

	private void write(Path path, int workerId) {
		try {
			List<String> lines = new ArrayList<>( workerLineCount );

			for ( int j = 1; j <= workerLineCount; j++ ) {
				lines.add( String.valueOf( workerId ^ j ) );
			}

			Files.write( path, lines, UTF_8, APPEND );
		}
		catch (IOException e) {
			LOGGER.error( "Write failed", e);
		}
	}
}
