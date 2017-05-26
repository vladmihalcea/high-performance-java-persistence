package com.vladmihalcea.book.hpjp.jdbc.transaction.locking.advisory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLReadWriteAdvisoryLocksTest extends AbstractPostgreSQLIntegrationTest {

	private static final String TEMP_FOLDER = System.getProperty( "java.io.tmpdir" );

	protected final ExecutorService executorService = Executors.newFixedThreadPool(3);

	private final CountDownLatch aliceLatch = new CountDownLatch( 1 );

	private final CountDownLatch bobLatch = new CountDownLatch( 1 );

	public static final int documentId = 123;

	private static final Path doc = Paths.get( TEMP_FOLDER, String.format( "document_%d.log", documentId ) );

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
		};
	}

	@Override
	public void init() {
		super.init();
		try {
			Files.deleteIfExists( doc );
			Files.createFile( doc );
		}
		catch (IOException e) {
			fail( e.getMessage() );
		}
	}

	@Override
	public void destroy() {
		try {
			Files.deleteIfExists( doc );
		}
		catch (IOException e) {
			fail( e.getMessage() );
		}
		super.destroy();
	}

	@Test
	public void testSharedBlocksWrite() {
		doInJDBC( aliceConnection -> {
			try {
				LOGGER.info( "Alice acquires a shared lock for document {}", documentId );
				acquireShareLock( aliceConnection, documentId );
				try {
					executorService.submit( () -> {
						doInJDBC( carolConnection -> {
							Thread.currentThread().setName( "Carol" );
							try {
								LOGGER.info( "Carol tries to acquire a shared lock for document {}", documentId );
								acquireShareLock( carolConnection, documentId );
								LOGGER.info( "Carol reads document {}", documentId );
								assertTrue(read( doc ).isEmpty() );
							}
							finally {
								LOGGER.info( "Carol releases lock for document {}", documentId );
								releaseShareLock( carolConnection, documentId );
							}
						});
					} ).get();
				}
				catch (InterruptedException|ExecutionException e) {
					fail(e.getMessage());
				}

				executorService.submit( () -> {
					Thread.currentThread().setName( "Bob" );
					doInJDBC( bobConnection -> {
						try {
							LOGGER.info( "Bob tries to acquire an exclusive lock for document {}", documentId );
							acquireExclusiveLock( bobConnection, documentId );
							LOGGER.info( "Bob writes to the document {}", documentId );
							write(
									doc,
									Arrays.asList(
											"High-Performance Java Persistence",
											"Vlad Mihalcea"
									)
							);
						}
						finally {
							releaseExclusiveLock( bobConnection, documentId );
						}
						bobLatch.countDown();
					} );
				} );

				LOGGER.info( "Alice sleeps for {} second", 1 );
				sleep( 1 * 1000 );
				LOGGER.info( "Alice reads the document {}", documentId );
				assertTrue(read( doc ).isEmpty() );
			}
			finally {
				LOGGER.info( "Alice releases the shared lock for document {}", documentId );
				releaseShareLock( aliceConnection, documentId );
			}
			awaitOnLatch( bobLatch );
		} );

	}

	@Test
	public void testExclusiveBlocksShared() {
		doInJDBC( aliceConnection -> {
			try {
				LOGGER.info( "Alice acquires an exclusive lock for document {}", documentId );
				acquireExclusiveLock( aliceConnection, documentId );

				executorService.submit( () -> {
					Thread.currentThread().setName( "Bob" );
					doInJDBC( bobConnection -> {
						try {
							LOGGER.info( "Bob tries to acquire a shared lock for document {}", documentId );
							acquireShareLock( bobConnection, documentId );
							LOGGER.info( "Bob reads the document {} which has {} lines", documentId, read( doc ).size() );
						}
						finally {
							releaseShareLock( bobConnection, documentId );
						}
						bobLatch.countDown();
					} );
				} );

				LOGGER.info( "Alice sleeps for {} second", 1 );
				sleep( 1 * 1000 );
				LOGGER.info( "Alice writes to the document {}", documentId );
				write(
						doc,
						Arrays.asList(
								"High-Performance Java Persistence",
								"Vlad Mihalcea"
						)
				);
			}
			finally {
				LOGGER.info( "Alice releases exclusive lock for document {}", documentId );
				releaseExclusiveLock( aliceConnection, documentId );
			}
			awaitOnLatch( bobLatch );
		} );
	}

	protected void acquireShareLock(Connection connection, int lockId) {
		try(PreparedStatement statement =
					connection.prepareStatement("select pg_advisory_lock_shared(?)")) {
			statement.setInt( 1, lockId );
			statement.executeQuery();
		}
		catch (SQLException e) {
			throw new IllegalStateException( e );
		}
	}

	protected void releaseShareLock(Connection connection, int lockId){
		try(PreparedStatement statement =
					connection.prepareStatement("select pg_advisory_unlock_shared(?)")) {
			statement.setInt( 1, lockId );
			statement.executeQuery();
		}
		catch (SQLException e) {
			throw new IllegalStateException( e );
		}
	}

	protected void acquireExclusiveLock(Connection connection, int lockId){
		try(PreparedStatement statement =
					connection.prepareStatement("select pg_advisory_lock(?)")) {
			statement.setInt( 1, lockId );
			statement.executeQuery();
		}
		catch (SQLException e) {
			throw new IllegalStateException( e );
		}
	}

	protected void releaseExclusiveLock(Connection connection, int lockId){
		try(PreparedStatement statement =
					connection.prepareStatement("select pg_advisory_unlock(?)")) {
			statement.setInt( 1, lockId );
			statement.executeQuery();
		}
		catch (SQLException e) {
			throw new IllegalStateException( e );
		}
	}

	private List<String> read(Path path) {
		try {
			return Files.readAllLines( path, UTF_8 );
		}
		catch (IOException e) {
			throw new IllegalArgumentException( e );
		}
	}

	private void write(Path path, List<String> lines) {
		try {
			Files.write( path, lines, UTF_8, APPEND );
		}
		catch (IOException e) {
			throw new IllegalArgumentException( e );
		}
	}
}
