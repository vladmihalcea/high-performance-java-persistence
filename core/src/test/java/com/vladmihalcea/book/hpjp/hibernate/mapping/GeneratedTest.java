package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class GeneratedTest extends AbstractSQLServerIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Hero.class
		};
	}

	@Test
	public void test() {
		doInJPA( entityManager -> {
			Hero heroine = new Hero();
			heroine.setId( 1L );

			heroine.setFirstName( "Agustina" );
			heroine.setMiddleName1( "Raimunda" );
			heroine.setMiddleName2( "María" );
			heroine.setMiddleName3( "Saragossa" );
			heroine.setLastName( "Domènech" );

			entityManager.persist( heroine );
			LOGGER.info("After entity persist action");
			entityManager.flush();

			assertEquals("Agustina Raimunda María Saragossa Domènech", heroine.getFullName());
		} );
		doInJPA( entityManager -> {
			Hero heroine = entityManager.find( Hero.class, 1L );
			heroine.setMiddleName1( null );
			heroine.setMiddleName2( null );
			heroine.setMiddleName3( null );
			heroine.setLastName( "de Aragón" );

			LOGGER.info("After entity update action");
			entityManager.flush();

			assertEquals("Agustina de Aragón", heroine.getFullName());
		} );
	}

	@Entity(name = "Hero")
	public static class Hero {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		private String middleName1;

		private String middleName2;

		private String middleName3;

		private String middleName4;

		private String middleName5;

		@Generated( value = GenerationTime.ALWAYS )
		@Column(columnDefinition =
			"AS CONCAT(" +
			"	COALESCE(firstName, ''), " +
			"	COALESCE(' ' + middleName1, ''), " +
			"	COALESCE(' ' + middleName2, ''), " +
			"	COALESCE(' ' + middleName3, ''), " +
			"	COALESCE(' ' + middleName4, ''), " +
			"	COALESCE(' ' + middleName5, ''), " +
			"	COALESCE(' ' + lastName, '') " +
			")")
		private String fullName;

		//Getters and setters omitted for brevity

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getMiddleName1() {
			return middleName1;
		}

		public void setMiddleName1(String middleName1) {
			this.middleName1 = middleName1;
		}

		public String getMiddleName2() {
			return middleName2;
		}

		public void setMiddleName2(String middleName2) {
			this.middleName2 = middleName2;
		}

		public String getMiddleName3() {
			return middleName3;
		}

		public void setMiddleName3(String middleName3) {
			this.middleName3 = middleName3;
		}

		public String getMiddleName4() {
			return middleName4;
		}

		public void setMiddleName4(String middleName4) {
			this.middleName4 = middleName4;
		}

		public String getMiddleName5() {
			return middleName5;
		}

		public void setMiddleName5(String middleName5) {
			this.middleName5 = middleName5;
		}

		public String getFullName() {
			return fullName;
		}

	}
}
