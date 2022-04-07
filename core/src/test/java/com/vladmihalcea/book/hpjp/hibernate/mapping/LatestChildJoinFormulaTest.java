package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.JoinFormula;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class LatestChildJoinFormulaTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Post.class,
			PostComment.class
		};
	}

	@Test
	public void test() {

		doInJPA( entityManager -> {
			Post post = new Post()
				.setId(1L)
				.setTitle("High-Performance Java Persistence");

			entityManager.persist(post);

			assertNull(post.getLatestComment());

			entityManager.persist(
				new PostComment()
		 		.setId(1L)
				.setPost(post)
				.setCreatedOn(
					Timestamp.valueOf(
						LocalDateTime.of(2016, 11, 2, 12, 33, 14)
					)
				)
				.setReview("Woohoo!")
			);

			entityManager.persist(
				new PostComment()
		 		.setId(2L)
				.setPost(post)
				.setCreatedOn(
					Timestamp.valueOf(
						LocalDateTime.of(2016, 11, 2, 15, 45, 58)
					)
				)
				.setReview("Finally!")
			);

			entityManager.persist(
				new PostComment()
		 		.setId(3L)
				.setPost(post)
				.setCreatedOn(
					Timestamp.valueOf(
						LocalDateTime.of(2017, 2, 16, 16, 10, 21)
					)
				)
				.setReview("Awesome!")
			);
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			PostComment latestComment = post.getLatestComment();

			assertEquals("Awesome!", latestComment.getReview());
		} );

		doInJPA( entityManager -> {
			List<Post> posts = entityManager.createQuery("""
				select p
				from Post p
				join fetch p.latestComment
				""", Post.class)
			.getResultList();

			assertEquals("Awesome!", posts.get(0).getLatestComment().getReview());
		} );

		doInJPA( entityManager -> {
			entityManager.persist(
				new Post()
					.setId(2L)
					.setTitle("High-Performance Java Persistence 2nd edition")
			);
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 2L);
			assertNull(post.getLatestComment());
		} );
	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinFormula("""
			(SELECT pc.id
			FROM post_comment pc
			WHERE pc.post_id = id
			ORDER BY pc.created_on DESC
			LIMIT 1)
			"""
		)
		private PostComment latestComment;

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

		public PostComment getLatestComment() {
			return latestComment;
		}
	}

	@Entity(name = "PostComment")
	@Table(name = "post_comment")
	public static class PostComment {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Post post;

		private String review;

		@Column(name = "created_on")
		@Temporal(TemporalType.TIMESTAMP)
		private Date createdOn;

		public Long getId() {
			return id;
		}

		public PostComment setId(Long id) {
			this.id = id;
			return this;
		}

		public Post getPost() {
			return post;
		}

		public PostComment setPost(Post post) {
			this.post = post;
			return this;
		}

		public String getReview() {
			return review;
		}

		public PostComment setReview(String review) {
			this.review = review;
			return this;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public PostComment setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
			return this;
		}
	}
}
