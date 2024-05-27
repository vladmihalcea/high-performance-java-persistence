package com.vladmihalcea.hpjp.hibernate.inheritance.discriminator.description;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@DiscriminatorValue("1")
public class Post extends Topic {

	private String content;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
