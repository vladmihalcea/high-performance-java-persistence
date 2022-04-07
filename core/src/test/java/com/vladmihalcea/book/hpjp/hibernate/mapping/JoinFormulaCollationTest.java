package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.JoinFormula;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JoinFormulaCollationTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Country.class,
			User.class
		};
	}

	@Test
	public void testLifecycle() {
		Country US = new Country();
		US.setCode( "US" );
		US.setName( "United States" );

		Country Romania = new Country();
		Romania.setCode( "RO" );
		Romania.setName( "Romania" );

		doInJPA( entityManager -> {
			entityManager.persist( US );
			entityManager.persist( Romania );
		} );

		doInJPA( entityManager -> {
			User user1 = new User( );
			user1.setId( 1L );
			user1.setFirstName( "John" );
			user1.setLastName( "Doe" );
			user1.setCountryCode( "us" );
			entityManager.persist( user1 );

			User user2 = new User( );
			user2.setId( 2L );
			user2.setFirstName( "Vlad" );
			user2.setLastName( "Mihalcea" );
			user2.setCountryCode( "Ro" );
			entityManager.persist( user2 );
		} );

		doInJPA( entityManager -> {
			User john = entityManager.find( User.class, 1L );
			assertEquals( US, john.getCountry());

			User vlad = entityManager.find( User.class, 2L );
			assertEquals( Romania, vlad.getCountry());
		} );
	}

	@Entity(name = "User")
	@Table(name = "users")
	public static class User {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		private String countryCode;

		@ManyToOne
		@JoinFormula( "(select c.code from Country c where UPPER(c.code) = UPPER(countryCode))" )
		private Country country;

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

		public String getCountryCode() {
			return countryCode;
		}

		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		public Country getCountry() {
			return country;
		}
	}

	@Entity(name = "Country")
	public static class Country {

		@Id
		private String code;

		private String name;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Country ) ) {
				return false;
			}
			Country country = (Country) o;
			return Objects.equals( getCode(), country.getCode() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( getCode() );
		}
	}
}
