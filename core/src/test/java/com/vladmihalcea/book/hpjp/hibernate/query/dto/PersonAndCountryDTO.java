package com.vladmihalcea.book.hpjp.hibernate.query.dto;

/**
 * @author Vlad Mihalcea
 */
public class PersonAndCountryDTO {

	private final Person person;

	private final String country;

	public PersonAndCountryDTO(Person person, String country) {
		this.person = person;
		this.country = country;
	}

	public Person getPerson() {
		return person;
	}

	public String getCountry() {
		return country;
	}
}
