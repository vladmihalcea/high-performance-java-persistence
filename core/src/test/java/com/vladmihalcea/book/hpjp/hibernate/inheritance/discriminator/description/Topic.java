package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator.description;

import java.util.Date;
import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "topic")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
	discriminatorType = DiscriminatorType.INTEGER,
	name = "topic_type_id",
	columnDefinition = "TINYINT(1)"
)
@DiscriminatorValue("0")
public class Topic {

	@Id
	@GeneratedValue
	private Long id;

	private String title;

	private String owner;

	@Temporal(TemporalType.TIMESTAMP)
	private Date createdOn = new Date();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "topic_type_id",
		insertable = false,
		updatable = false
	)
	private TopicType type;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public TopicType getType() {
		return type;
	}
}
