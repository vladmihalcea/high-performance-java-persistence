package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class BatchTest extends AbstractTest {

	private static final Logger log = Logger.getLogger( BatchTest.class );

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Post.class
		};
	}

	@Test
	public void testScroll() {
		withBatchAndSessionManagement();
	}

	private void withBatch() {
		int entityCount = 20;
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();
			entityManager.unwrap(Session.class).setJdbcBatchSize(10);

			txn = entityManager.getTransaction();
			txn.begin();

			int entityManagerBatchSize = 20;

			for ( long i = 0; i < entityCount; ++i ) {
				Post person = new Post( i, String.format( "Post nr %d", i ));
				entityManager.persist( person );

				if ( i > 0 && i % entityManagerBatchSize == 0 ) {
					entityManager.flush();
					entityManager.clear();
				}
			}

			txn.commit();
		} catch (RuntimeException e) {
			if ( txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	private void withBatchAndSessionManagement() {
		int entityCount = 20;

		doInJPA(entityManager -> {
			entityManager.unwrap(Session.class).setJdbcBatchSize(10);

			for ( long i = 0; i < entityCount; ++i ) {
				Post person = new Post( i, String.format( "Post nr %d", i ));
				entityManager.persist( person );
			}
		});
	}

	private void withBatchAndResetBackToGlobalSetting() {
		EntityManager entityManager = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();
			entityManager.getTransaction().begin();


		} finally {
			if (entityManager != null) {
				entityManager.getTransaction().rollback();
				entityManager.close();
			}
		}
	}

	@Entity(name = "Post")
	public static class Post {

		@Id
		private Long id;

		private String name;

		public Post() {}

		public Post(long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
