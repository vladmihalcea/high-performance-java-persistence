package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "topic_type")
public class TopicType {

	@Id
	@Column(columnDefinition = "TINYINT(1)")
	private Byte id;

	private String name;

	private String description;

	public Byte getId() {
		return id;
	}

	public void setId(Byte id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "TopicType{" +
				"id=" + id +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				'}';
	}
}
