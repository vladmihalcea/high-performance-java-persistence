package com.vladmihalcea.book.hpjp.hibernate.mapping.softdelete;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class SoftDeleteTest extends AbstractTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Post.class,
			PostComment.class,
		};
	}

	@Test
	public void testRemoveAndFindPost() {
		doInJPA( entityManager -> {
			Post post = new Post();
			post.setId(1L);
			post.setTitle("High-Performance Java Persistence");
			entityManager.persist(post);
		} );
		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			entityManager.remove(post);
		} );
		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertNull(post);
		} );
	}

	@Test
	public void testRemoveAndFindPostComment() {
		doInJPA( entityManager -> {
			Post post = new Post();
			post.setId(1L);
			post.setTitle("High-Performance Java Persistence");
			entityManager.persist(post);

			PostComment comment1 = new PostComment();
			comment1.setId(1L);
			comment1.setReview("Great!");
			post.addComment(comment1);

			PostComment comment2 = new PostComment();
			comment2.setId(2L);
			comment2.setReview("Excellent!");
			post.addComment(comment2);
		} );
		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			post.removeComment(post.getComments().get(0));
		} );
		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(1, post.getComments().size());
		} );
	}

	@Entity(name = "Post")
	@Table(name = "post")
	@Loader(namedQuery = "findPostById")
	@NamedQuery(name = "findPostById", query = "select p from Post p where p.id = ? and p.deleted = false")
	@SQLDelete(sql = "UPDATE post set deleted = true where id = ?")
	public static class Post {

		@Id
		private Long id;

		private boolean deleted;

		private String title;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
		@Where(clause = "deleted = false")
		private List<PostComment> comments = new ArrayList<>();

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

		public List<PostComment> getComments() {
			return comments;
		}

		public void addComment(PostComment comment) {
			comments.add(comment);
			comment.setPost(this);
		}

		public void removeComment(PostComment comment) {
			comments.remove(comment);
			comment.setPost(null);
		}
	}

	@Entity(name = "PostComment")
	@Table(name = "post_comment")
	@Loader(namedQuery = "findPostCommentById")
	@NamedQuery(name = "findPostCommentById", query = "select pc from PostComment pc where pc.id = ? and pc.deleted = false")
	@SQLDelete(sql = "UPDATE post_comment set deleted = true where id = ?")
	public static class PostComment {

		@Id
		private Long id;

		private boolean deleted;

		@ManyToOne(fetch = FetchType.LAZY)
		private Post post;

		private String review;

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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof PostComment)) return false;
			PostComment that = (PostComment) o;
			return Objects.equals(getId(), that.getId());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getId());
		}
	}
}
