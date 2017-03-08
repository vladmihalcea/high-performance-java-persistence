package com.vladmihalcea.book.hpjp.hibernate.mapping.softdelete;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class SoftDeleteTest extends AbstractMySQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Post.class,
			PostDetails.class,
			PostComment.class,
			Tag.class
		};
	}

	@Override
	public void init() {
		super.init();

		doInJPA( entityManager -> {
			Tag javaTag = new Tag();
			javaTag.setId("Java");
			entityManager.persist(javaTag);

			Tag jpaTag = new Tag();
			jpaTag.setId("JPA");
			entityManager.persist(jpaTag);

			Tag hibernateTag = new Tag();
			hibernateTag.setId("Hibernate");
			entityManager.persist(hibernateTag);

			Tag miscTag = new Tag();
			miscTag.setId("Misc");
			entityManager.persist(miscTag);
		} );
	}

	@Test
	public void testRemoveTag() {
		doInJPA( entityManager -> {
			Post post = new Post();
			post.setId(1L);
			post.setTitle("High-Performance Java Persistence");

			entityManager.persist(post);

			post.addTag(entityManager.getReference(Tag.class, "Java"));
			post.addTag(entityManager.getReference(Tag.class, "Hibernate"));
			post.addTag(entityManager.getReference(Tag.class, "Misc"));
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(3, post.getTags().size());
		} );

		doInJPA( entityManager -> {
			Tag miscTag = entityManager.getReference(Tag.class, "Misc");
			entityManager.remove(miscTag);
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(2, post.getTags().size());
		} );

		doInJPA( entityManager -> {
			//That would not work without @Loader(namedQuery = "findTagById")
			assertNull(entityManager.find(Tag.class, "Misc"));
		} );

		doInJPA( entityManager -> {
			List<Tag> tags = entityManager.createQuery("select t from Tag t", Tag.class).getResultList();
			//That would not work without @Where(clause = "deleted = false")
			assertEquals(3, tags.size());
		} );
	}

	@Test
	public void testRemovePostDetails() {
		doInJPA( entityManager -> {
			Post post = new Post();
			post.setId(1L);
			post.setTitle("High-Performance Java Persistence");

			PostDetails postDetails = new PostDetails();
			postDetails.setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2016, 11, 2, 12, 0, 0)));
			post.addDetails(postDetails);

			entityManager.persist(post);

			post.addTag(entityManager.getReference(Tag.class, "Java"));
			post.addTag(entityManager.getReference(Tag.class, "Hibernate"));
			post.addTag(entityManager.getReference(Tag.class, "Misc"));

			PostComment comment1 = new PostComment();
			comment1.setId(1L);
			comment1.setReview("Great!");
			post.addComment(comment1);

			PostComment comment2= new PostComment();
			comment2.setId(2L);
			comment2.setReview("To read");
			post.addComment(comment2);
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertNotNull(post.getDetails());
			post.removeDetails();
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertNull(post.getDetails());
		} );

		doInJPA( entityManager -> {
			assertNull(entityManager.find(PostDetails.class, 1L));
		} );
	}

	@Test
	public void testRemovePostComment() {
		doInJPA( entityManager -> {
			Post post = new Post();
			post.setId(1L);
			post.setTitle("High-Performance Java Persistence");

			PostDetails postDetails = new PostDetails();
			postDetails.setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2016, 11, 2, 12, 0, 0)));
			post.addDetails(postDetails);

			entityManager.persist(post);

			post.addTag(entityManager.getReference(Tag.class, "Java"));
			post.addTag(entityManager.getReference(Tag.class, "Hibernate"));
			post.addTag(entityManager.getReference(Tag.class, "Misc"));

			PostComment comment1 = new PostComment();
			comment1.setId(1L);
			comment1.setReview("Great!");
			post.addComment(comment1);

			PostComment comment2= new PostComment();
			comment2.setId(2L);
			comment2.setReview("To read");
			post.addComment(comment2);
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(2, post.getComments().size());
			assertNotNull(entityManager.find(PostComment.class, 2L));
			post.removeComment(post.getComments().get(1));
		} );

		doInJPA( entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(1, post.getComments().size());
			assertNull(entityManager.find(PostComment.class, 2L));
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
	@SQLDelete(sql =
		"UPDATE post " +
		"SET deleted = true " +
		"WHERE id = ?")
	@Loader(namedQuery = "findPostById")
	@NamedQuery(name = "findPostById", query =
		"SELECT p " +
		"FROM Post p " +
		"WHERE " +
		"	p.id = ? AND " +
		"	p.deleted = false")
	@Where(clause = "deleted = false")
	public static class Post extends BaseEntity {

		@Id
		private Long id;

		private String title;

		@OneToMany(
			mappedBy = "post",
			cascade = CascadeType.ALL,
			orphanRemoval = true
		)
		private List<PostComment> comments = new ArrayList<>();

		@OneToOne(
			mappedBy = "post",
			cascade = CascadeType.ALL,
			orphanRemoval = true,
			fetch = FetchType.LAZY
		)
		private PostDetails details;

		@ManyToMany
		@JoinTable(
			name = "post_tag",
			joinColumns = @JoinColumn(name = "post_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id")
		)
		private List<Tag> tags = new ArrayList<>();

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

		public PostDetails getDetails() {
			return details;
		}

		public List<Tag> getTags() {
			return tags;
		}

		public void addComment(PostComment comment) {
			comments.add(comment);
			comment.setPost(this);
		}

		public void removeComment(PostComment comment) {
			comments.remove(comment);
			comment.setPost(null);
		}

		public void addDetails(PostDetails details) {
			this.details = details;
			details.setPost(this);
		}

		public void removeDetails() {
			this.details.setPost(null);
			this.details = null;
		}

		public void addTag(Tag tag) {
			tags.add(tag);
		}
	}

	@Entity(name = "PostDetails")
	@Table(name = "post_details")
	@SQLDelete(sql =
		"UPDATE post_details " +
		"SET deleted = true " +
		"WHERE id = ?")
	@Loader(namedQuery = "findPostDetailsById")
	@NamedQuery(name = "findPostDetailsById", query =
		"SELECT pd " +
		"FROM PostDetails pd " +
		"WHERE " +
		"	pd.id = ? AND " +
		"	pd.deleted = false")
	@Where(clause = "deleted = false")
	public static class PostDetails extends BaseEntity {

		@Id
		private Long id;

		@Column(name = "created_on")
		private Date createdOn;

		@Column(name = "created_by")
		private String createdBy;

		public PostDetails() {
			createdOn = new Date();
		}

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "id")
		@MapsId
		private Post post;

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

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public String getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(String createdBy) {
			this.createdBy = createdBy;
		}
	}

	@Entity(name = "PostComment")
	@Table(name = "post_comment")
	@SQLDelete(sql =
		"UPDATE post_comment " +
		"SET deleted = true " +
		"WHERE id = ?")
	@Loader(namedQuery = "findPostCommentById")
	@NamedQuery(name = "findPostCommentById", query =
		"SELECT pc " +
		"from PostComment pc " +
		"WHERE " +
		"	pc.id = ? AND " +
		"	pc.deleted = false")
	@Where(clause = "deleted = false")
	public static class PostComment extends BaseEntity {

		@Id
		private Long id;

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
	}

	@Entity(name = "Tag")
	@Table(name = "tag")
	@SQLDelete(sql =
		"UPDATE tag " +
		"SET deleted = true " +
		"WHERE id = ?")
	@Loader(namedQuery = "findTagById")
	@NamedQuery(name = "findTagById", query =
		"SELECT t " +
		"FROM Tag t " +
		"WHERE " +
		"	t.id = ? AND " +
		"	t.deleted = false")
	@Where(clause = "deleted = false")
	public static class Tag extends BaseEntity {

		@Id
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public static abstract class BaseEntity {

		private boolean deleted;
	}
}
