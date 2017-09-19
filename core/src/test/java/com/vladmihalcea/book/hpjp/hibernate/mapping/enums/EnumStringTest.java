package com.vladmihalcea.book.hpjp.hibernate.mapping.enums;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EnumStringTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Post.class
		};
	}

	@Test
	public void test() {
		doInJPA( entityManager -> {
			Post post = new Post();
			post.setId( 1L );
			post.setTitle( "High-Performance Java Persistence" );
			post.setStatus( PostStatus.PENDING );
			entityManager.persist( post );
		} );
	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		private Long id;

		private String title;

		@Enumerated(EnumType.STRING)
		@Column(length = 8)
		private PostStatus status;

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

		public PostStatus getStatus() {
			return status;
		}

		public void setStatus(PostStatus status) {
			this.status = status;
		}
	}

	public enum PostStatus {
		PENDING,
		APPROVED,
		SPAM
	}
}
