package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author Vlad Mihalcea
 */
@NamedNativeQuery(
	name = "PostDTO",
	query =
		"SELECT " +
		"       p.id AS id, " +
		"       p.title AS title " +
		"FROM Post p " +
		"WHERE p.created_on > :fromTimestamp",
	resultSetMapping = "PostDTO"
)
@SqlResultSetMapping(
	name = "PostDTO",
	classes = @ConstructorResult(
		targetClass = PostDTO.class,
		columns = {
			@ColumnResult(name = "id"),
			@ColumnResult(name = "title")
		}
	)
)
@Entity(name = "Post")
public class Post {

	@Id
	private Long id;

	private String title;

	@Column(name = "created_on")
	private Timestamp createdOn;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@Column(name = "updated_by")
	private String updatedBy;

	@Version
	private Integer version;

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

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
}
