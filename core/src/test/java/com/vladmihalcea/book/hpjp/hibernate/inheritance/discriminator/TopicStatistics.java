package com.vladmihalcea.book.hpjp.hibernate.inheritance.discriminator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "topic_statistics")
public class TopicStatistics {

	@Id
	@GeneratedValue
	private Long id;

	@OneToOne
	@JoinColumn(name = "id")
	@MapsId
	private Topic topic;

	private long views;

	public TopicStatistics() {
	}

	public TopicStatistics(Topic topic) {
		this.topic = topic;
	}

	public Long getId() {
		return id;
	}

	public Topic getTopic() {
		return topic;
	}

	public long getViews() {
		return views;
	}

	public void incrementViews() {
		this.views++;
	}
}
