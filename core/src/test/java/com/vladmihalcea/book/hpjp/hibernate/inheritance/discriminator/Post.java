package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post")
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
