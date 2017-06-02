package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "board")
public class Board {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	//Only useful for the sake of seeing the queries being generated.
	@OneToMany(mappedBy = "board")
	private List<Topic> topics = new ArrayList<>();

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

	public List<Topic> getTopics() {
		return topics;
	}
}
