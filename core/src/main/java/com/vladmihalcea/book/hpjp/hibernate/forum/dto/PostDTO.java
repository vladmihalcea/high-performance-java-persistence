package com.vladmihalcea.book.hpjp.hibernate.forum.dto;

/**
 * @author Vlad Mihalcea
 */
public class PostDTO {

	private final Long id;

	private final String title;

	public PostDTO(Number id, String title) {
		this.id = id.longValue();
		this.title = title;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}
}
