/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
			Person.class
		};
	}

	@Test
	public void testScroll() {
		withBatch();
	}

	private void withBatch() {
		int entityCount = 100;
		//tag::batch-session-batch-insert-example[]
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();
			entityManager.unwrap(Session.class).setJdbcBatchSize(10);

			txn = entityManager.getTransaction();
			txn.begin();

			int batchSize = 25;

			for ( long i = 0; i < entityCount; ++i ) {
				Person person = new Person( i, String.format( "Person %d", i ));
				entityManager.persist( person );

				if ( i % batchSize == 0 ) {
					//flush a batch of inserts and release memory
					entityManager.flush();
					entityManager.clear();
				}
			}

			txn.commit();
		} catch (RuntimeException e) {
			if ( txn != null && txn.isActive()) txn.rollback();
				throw e;
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
		//end::batch-session-batch-insert-example[]
	}

	private void processPerson(Person Person) {
		if ( Person.getId() % 1000 == 0 ) {
			log.infof( "Processing [%s]", Person.getName());
		}
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		public Person() {}

		public Person(long id, String name) {
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
