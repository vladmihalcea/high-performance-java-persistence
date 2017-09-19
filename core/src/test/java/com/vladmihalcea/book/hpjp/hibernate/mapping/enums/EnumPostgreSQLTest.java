package com.vladmihalcea.book.hpjp.hibernate.mapping.enums;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Tuple;
import javax.sql.DataSource;
import javax.xml.crypto.Data;

import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class EnumPostgreSQLTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Post.class,
		};
	}

	public void init() {
		DataSource dataSource = newDataSource();
		try (Connection connection = dataSource.getConnection()) {
			try(Statement statement = connection.createStatement()) {
				try {
					statement.executeUpdate(
						"DROP TYPE post_status_info"
					);
				}
				catch (SQLException ignore) {
				}
				statement.executeUpdate(
					"CREATE TYPE post_status_info AS ENUM ('PENDING', 'APPROVED', 'SPAM')"
				);
			}
		}
		catch (SQLException e) {
			fail(e.getMessage());
		}
		super.init();
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

		doInJPA( entityManager -> {
			Post post = entityManager.find( Post.class, 1L );
			assertEquals( PostStatus.PENDING, post.getStatus() );
		} );
	}

	@Entity(name = "Post")
	@Table(name = "post")
	@TypeDef(
		name = "pgsql_enum",
		typeClass = PostgreSQLEnumType.class
	)
	public static class Post {

		@Id
		private Long id;

		private String title;

		@Enumerated(EnumType.STRING)
		@Column(columnDefinition = "post_status_info")
		@Type( type = "pgsql_enum" )
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
