package com.vladmihalcea.book.hpjp.hibernate.sp;

import com.vladmihalcea.book.hpjp.util.AbstractOracleIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.jboss.logging.Logger;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class OracleCustomSQLWithStoredProcedureTest extends AbstractOracleIntegrationTest {

	private static final Logger log = Logger.getLogger( OracleCustomSQLWithStoredProcedureTest.class );

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Person.class
		};
	}

	public void init() {
		super.init();
		doInJPA(entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try(Statement statement = connection.createStatement(); ) {
					statement.executeUpdate( "ALTER TABLE person ADD valid NUMBER(1) DEFAULT 0 NOT NULL" );
					statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_delete_person ( " +
						"   personId IN NUMBER ) " +
						"AS  " +
						"BEGIN " +
						"    UPDATE person SET valid = 0 WHERE id = personId; " +
						"END;"
					);}
			} );
		});
	}

	@Test
	public void test_sql_custom_crud() {

		Person _person = doInJPA(entityManager -> {
			Person person = new Person();
			person.setName( "John Doe" );
			entityManager.persist( person );
			return person;
		} );

		doInJPA(entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertNotNull(person);
			entityManager.remove( person );
		} );

		doInJPA(entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertNull(person);
		} );
	}

	@Entity(name = "Person")
	@SQLInsert(
		sql = "INSERT INTO person (name, id, valid) VALUES (?, ?, 1) ",
		check = ResultCheckStyle.COUNT
	)
	@SQLDelete(
		sql =   "{ call sp_delete_person( ? ) } ",
		callable = true
	)
	@Loader(namedQuery = "find_valid_person")
	@NamedNativeQueries({
		@NamedNativeQuery(
			name = "find_valid_person",
			query = "SELECT id, name " +
					"FROM person " +
					"WHERE id = ? and valid = 1",
			resultClass = Person.class
		)
	})
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
