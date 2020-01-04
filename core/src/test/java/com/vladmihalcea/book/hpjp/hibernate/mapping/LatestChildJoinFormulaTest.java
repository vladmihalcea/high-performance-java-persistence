package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.annotations.JoinFormula;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class LatestChildJoinFormulaTest extends AbstractMySQLIntegrationTest {

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
			Post post = new Post();
			post.setId(1L);
			post.setTitle("High-Performance Java Persistence");
			entityManager.persist(post);
			assertNull(post.getLatestComment());

			PostComment comment1 = new PostComment();
			comment1.setId(1L);
			comment1.setPost(post);
			comment1.setCreatedOn(Timestamp.valueOf(
				LocalDateTime.of(2016, 11, 2, 12, 33, 14)
			));
			comment1.setReview("Woohoo!");
			entityManager.persist(comment1);

			PostComment comment2 = new PostComment();
			comment2.setId(2L);
			comment2.setPost(post);
			comment2.setCreatedOn(Timestamp.valueOf(
					LocalDateTime.of(2016, 11, 2, 15, 45, 58)
			));
			comment2.setReview("Finally!");
			entityManager.persist(comment2);

			PostComment comment3 = new PostComment();
			comment3.setId(3L);
			comment3.setPost(post);
			comment3.setCreatedOn(Timestamp.valueOf(
					LocalDateTime.of(2017, 2, 16, 16, 10, 21)
			));
			comment3.setReview("Awesome!");
			entityManager.persist(comment3);
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			PostComment latestComment = post.getLatestComment();

			assertEquals("Awesome!", latestComment.getReview());
		} );

		doInJPA( entityManager -> {
			List<Post> posts = entityManager.createQuery(
				"select p " +
				"from Post p " +
				"join fetch p.latestComment", Post.class)
			.getResultList();

			assertEquals("Awesome!", posts.get(0).getLatestComment().getReview());
		} );

		doInJPA( entityManager -> {
			Post post = new Post();
			post.setId(2L);
			post.setTitle("High-Performance Java Persistence 2nd edition");
			entityManager.persist(post);
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 2L);
			assertNull(post.getLatestComment());
		} );

        doInJPA( entityManager -> {
            List<Post> postsWithoutLatestComment = entityManager.createQuery(
                    "select p from Post p", Post.class)
                    .getResultList();

            assertEquals(2, postsWithoutLatestComment.size());

            List<Post> postsWithLatestComment = entityManager.createQuery(
                    "select p from Post p join fetch p.latestComment", Post.class)
                    .getResultList();

            assertEquals(2, postsWithLatestComment.size());
        });
	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinFormula("(" +
			"SELECT pc.id " +
			"FROM post_comment pc " +
			"WHERE pc.post_id = id " +
			"ORDER BY pc.created_on DESC " +
			"LIMIT 1" +
		")")
		private PostComment latestComment;

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

		public void setId(Long id) {
			this.id = id;
		}

		public Post getPost() {
			return post;
		}

		public void setPost(Post post) {
			this.post = post;
		}

		public String getReview() {
			return review;
		}

		public void setReview(String review) {
			this.review = review;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}
	}
}
