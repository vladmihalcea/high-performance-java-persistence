package com.vladmihalcea.book.hpjp.hibernate.query.dto;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Person")
public class Person {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private String locale;

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

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}
}
