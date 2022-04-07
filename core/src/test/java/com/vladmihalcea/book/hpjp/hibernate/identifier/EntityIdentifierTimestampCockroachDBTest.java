package com.vladmihalcea.book.hpjp.hibernate.identifier;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.Session;

import org.junit.Ignore;
import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractCockroachDBIntegrationTest;

import static org.junit.Assert.assertEquals;

public class EntityIdentifierTimestampCockroachDBTest extends AbstractCockroachDBIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
				Post.class,
		};
	}

	@Override
	protected boolean nativeHibernateSessionFactoryBootstrap() {
		return false;
	}

	@Test
	@Ignore
	public void test() {
		doInJPA( entityManager -> {
			entityManager.unwrap( Session.class ).doWork( connection -> {
				try(PreparedStatement preparedStatement = connection.prepareStatement(
					"INSERT INTO post (title, createdOn) " +
						"VALUES (?, ?)")
				) {
					int index = 0;
					preparedStatement.setString(
							++index,
							"High-Performance Java Persistence"
					);
					preparedStatement.setTimestamp(
							++index,
							new Timestamp( System.currentTimeMillis() )
					);
					int updateCount = preparedStatement.executeUpdate();

					assertEquals( 1, updateCount );
				}
			} );
		} );
	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(columnDefinition = "timestamptz")
		@Temporal(TemporalType.TIMESTAMP)
		private Date createdOn;

		private String title;

		public Post() {
		}

		public Post(String title) {
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}