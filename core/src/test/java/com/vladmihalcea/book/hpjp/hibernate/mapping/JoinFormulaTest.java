package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.JoinFormula;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class JoinFormulaTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Language.class,
			Country.class,
			Account.class
		};
	}

	@Test
	public void test() {

		doInJDBC(connection -> {
			try(Statement statement = connection.createStatement()) {
				statement.executeUpdate("CREATE INDEX account_language_idx ON Account (REGEXP_REPLACE(locale, '(\\w+)_.*', '\\1'))");
				statement.executeUpdate("CREATE INDEX account_country_idx ON Account (REGEXP_REPLACE(locale, '\\w+_(\\w+)[_]?', '\\1'))");
			}
		});

		Country _US = new Country();
		_US.setId( "US" );
		_US.setName( "United States" );
		_US.setVatRate(0.1);

		Country _UK = new Country();
		_UK.setId( "UK" );
		_UK.setName( "United Kingdom" );
		_UK.setVatRate(0.2);

		Country _Spain = new Country();
		_Spain.setId( "ES" );
		_Spain.setName( "Spain" );
		_Spain.setVatRate(0.21);

		Country _Mexico = new Country();
		_Mexico.setId( "MX" );
		_Mexico.setName( "Mexico" );
		_Mexico.setVatRate(0.16);

		Language _English = new Language();
		_English.setId( "en" );
		_English.setName( "English" );

		Language _Spanish = new Language();
		_Spanish.setId( "es" );
		_Spanish.setName( "Spanish" );

		doInJPA( entityManager -> {
			entityManager.persist( _US );
			entityManager.persist( _UK );
			entityManager.persist( _Spain );
			entityManager.persist( _Mexico );
			entityManager.persist( _English );
			entityManager.persist( _Spanish );
		} );

		doInJPA( entityManager -> {
			Account account1 = new Account( );
			account1.setId( 1L );
			account1.setCredit( 5000d );
			account1.setRate( 1.25 / 100 );
			account1.setLocale( Locale.US );
			entityManager.persist( account1 );

			Account account2 = new Account( );
			account2.setId( 2L );
			account2.setCredit( 200d );
			account2.setRate( 1.25 / 100 );
			account2.setLocale( new Locale( "es", "MX" ) );
			entityManager.persist( account2 );
		} );

		doInJPA( entityManager -> {
			LOGGER.info("Fetch first Account");
			Account account1 = entityManager.find( Account.class, 1L );
			assertEquals( _English, account1.getLanguage());
			assertEquals( _US, account1.getCountry());

			LOGGER.info("Fetch second Account");
			Account account2 = entityManager.find( Account.class, 2L );
			assertEquals( _Spanish, account2.getLanguage());
			assertEquals( _Mexico, account2.getCountry());
		} );

		doInJPA( entityManager -> {
			Account account1 = entityManager.createQuery(
				"select a " +
				"from Account a " +
				"join a.language l " +
				"join a.country c " +
				"where a.id = :accountId", Account.class )
			.setParameter("accountId", 1L)
			.getSingleResult();

			assertEquals( _English, account1.getLanguage());
			assertEquals( _US, account1.getCountry());
		} );
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		private Double credit;

		private Double rate;

		private Locale locale;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinFormula( "REGEXP_REPLACE(locale, '\\w+_(\\w+)[_]?', '\\1')" )
		private Country country;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinFormula( "REGEXP_REPLACE(locale, '(\\w+)_.*', '\\1')" )
		private Language language;
		
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Double getCredit() {
			return credit;
		}

		public void setCredit(Double credit) {
			this.credit = credit;
		}

		public Double getRate() {
			return rate;
		}

		public void setRate(Double rate) {
			this.rate = rate;
		}

		public Locale getLocale() {
			return locale;
		}

		public void setLocale(Locale locale) {
			this.locale = locale;
		}

		public Country getCountry() {
			return country;
		}

		public Language getLanguage() {
			return language;
		}

	}

	@Entity(name = "Language")
	public static class Language {

		@Id
		private String id;

		private String name;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
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
			if ( !( o instanceof Language ) ) {
				return false;
			}
			Language language = (Language) o;
			return Objects.equals( getId(), language.getId() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( getId() );
		}
	}

	@Entity(name = "Country")
	public static class Country {

		@Id
		private String id;

		private String name;

		private double vatRate;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public double getVatRate() {
			return vatRate;
		}

		public void setVatRate(double vatRate) {
			this.vatRate = vatRate;
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
			return Objects.equals( getId(), country.getId() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( getId() );
		}
	}
}
