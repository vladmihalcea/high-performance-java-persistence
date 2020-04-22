package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@NamedQuery(
	name = "PostDTOEntityQuery",
	query = """
		select new
			com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO(
				p.id,
				p.title
			)
		from Post p
		"""
)
@NamedNativeQuery(
	name = "PostDTONativeQuery",
	query = """
		SELECT
		   p.id AS id,
		   p.title AS title
		FROM Post p
		""",
	resultSetMapping = "PostDTOMapping"
)
@SqlResultSetMapping(
	name = "PostDTOMapping",
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
	private LocalDateTime createdOn;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "updated_on")
	private LocalDateTime updatedOn;

	@Column(name = "updated_by")
	private String updatedBy;

	@Version
	private Integer version;

	public Long getId() {
		return id;
	}

	public Post setId(Long id) {
		this.id = id;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public Post setTitle(String title) {
		this.title = title;
		return this;
	}

	public LocalDateTime getCreatedOn() {
		return createdOn;
	}

	public Post setCreatedOn(LocalDateTime createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public Post setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public LocalDateTime getUpdatedOn() {
		return updatedOn;
	}

	public Post setUpdatedOn(LocalDateTime updatedOn) {
		this.updatedOn = updatedOn;
		return this;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public Post setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
		return this;
	}

	public Integer getVersion() {
		return version;
	}

	public Post setVersion(Integer version) {
		this.version = version;
		return this;
	}
}
