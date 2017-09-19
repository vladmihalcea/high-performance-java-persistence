package com.vladmihalcea.book.hpjp.hibernate.mapping.enums;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Tuple;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EnumOrdinalTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Post.class,
			PostStatusInfo.class
		};
	}

	@Test
	public void test() {
		doInJPA( entityManager -> {
			PostStatusInfo pending = new PostStatusInfo();
			pending.setId( PostStatus.PENDING.ordinal() );
			pending.setName( PostStatus.PENDING.name() );
			pending.setDescription( "Posts waiting to be approved by the admin" );
			entityManager.persist( pending );

			PostStatusInfo approved = new PostStatusInfo();
			approved.setId( PostStatus.APPROVED.ordinal() );
			approved.setName( PostStatus.APPROVED.name() );
			approved.setDescription( "Posts approved by the admin" );
			entityManager.persist( approved );

			PostStatusInfo spam = new PostStatusInfo();
			spam.setId( PostStatus.SPAM.ordinal() );
			spam.setName( PostStatus.SPAM.name() );
			spam.setDescription( "Posts rejected as spam" );
			entityManager.persist( spam );
		} );

		doInJPA( entityManager -> {
			Post post = new Post();
			post.setId( 1L );
			post.setTitle( "High-Performance Java Persistence" );
			post.setStatus( PostStatus.PENDING );
			entityManager.persist( post );

			Tuple tuple = (Tuple) entityManager
			.createNativeQuery(
					"SELECT " +
					"    p.id, " +
					"    p.title, " +
					"    p.status, " +
					"    psi.name, " +
					"    psi.description " +
					"FROM post p " +
					"INNER JOIN post_status_info psi ON p.status = psi.id " +
					"WHERE p.id = :postId", Tuple.class
			)
			.setParameter( "postId", 1L )
			.getSingleResult();

			assertEquals("PENDING", tuple.get( "name" ));
			assertEquals("Posts waiting to be approved by the admin", tuple.get( "description" ));
		} );
	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		private Long id;

		private String title;

		@Enumerated
		@Column(columnDefinition = "smallint")
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

	@Entity(name = "PostStatusInfo")
	@Table(name = "post_status_info")
	public static class PostStatusInfo {

		@Id
		@Column(columnDefinition = "smallint")
		private Integer id;

		private String name;

		private String description;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
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
	}

	public enum PostStatus {
		PENDING,
		APPROVED,
		SPAM
	}
}
