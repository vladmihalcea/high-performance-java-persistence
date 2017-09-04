package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.hibernate;

/**
 * @author Vlad Mihalcea
 */
public class PostDTO {

	private Long id;

	private String title;

	public Long getId() {
		return id;
	}

	public void setId(Number id) {
		this.id = id.longValue();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
