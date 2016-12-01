package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.JoinFormula;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Locale;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JoinFormulaTest extends AbstractTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Language.class,
			Country.class,
			Account.class
		};
	}

	@Test
	public void testLifecycle() {
		Country us = new Country();
		us.setId( "US" );
		us.setName( "United States" );

		Country uk = new Country();
		uk.setId( "EK" );
		uk.setName( "United Kingdom" );

		Country spain = new Country();
		spain.setId( "ES" );
		spain.setName( "Spain" );

		Country mexico = new Country();
		mexico.setId( "MX" );
		mexico.setName( "Mexico" );

		Language english = new Language();
		english.setId( "en" );
		english.setName( "English" );

		Language spanish = new Language();
		spanish.setId( "es" );
		spanish.setName( "Spanish" );

		doInJPA( entityManager -> {
			entityManager.persist( us );
			entityManager.persist( uk );
			entityManager.persist( spain );
			entityManager.persist( mexico );
			entityManager.persist( english );
			entityManager.persist( spanish );
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
			Account account = entityManager.find( Account.class, 1L );
			assertEquals( english, account.getLanguage());
			assertEquals( us, account.getCountry());
		} );
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		private Double credit;

		private Double rate;

		private Locale locale;

		@ManyToOne
		@JoinFormula( "SUBSTRING(locale, 4)" )
		private Country country;

		@ManyToOne
		@JoinFormula( "SUBSTRING(locale, 1, 2)" )
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
