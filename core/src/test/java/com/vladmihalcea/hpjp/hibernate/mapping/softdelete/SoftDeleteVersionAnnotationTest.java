package com.vladmihalcea.hpjp.hibernate.mapping.softdelete;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SoftDelete;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class SoftDeleteVersionAnnotationTest extends AbstractTest {

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
	public void afterInit() {
		doInJPA( entityManager -> {
			entityManager.persist(
				new Tag().setName("Java")
			);

			entityManager.persist(
				new Tag().setName("JPA")
			);

			entityManager.persist(
				new Tag().setName("Hibernate")
			);
			
			entityManager.persist(
				new Tag().setName("Misc")
			);
		} );
	}

	@Test
	public void testRemoveTag() {
		doInJPA(entityManager -> {
			Post post = new Post();
			post.setId(1L);
			post.setTitle("High-Performance Java Persistence");

			entityManager.persist(post);

			post.addTag(entityManager.unwrap(Session.class).bySimpleNaturalId(Tag.class).getReference("Java"));
			post.addTag(entityManager.unwrap(Session.class).bySimpleNaturalId(Tag.class).getReference("Hibernate"));
			post.addTag(entityManager.unwrap(Session.class).bySimpleNaturalId(Tag.class).getReference("Misc"));
		});

		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(3, post.getTags().size());
		});

		Tag _miscTag = doInJPA(entityManager -> {
			Tag miscTag = entityManager.unwrap(Session.class)
				.bySimpleNaturalId(Tag.class)
				.getReference("Misc");

			entityManager.remove(miscTag);

			return miscTag;
		});

		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(2, post.getTags().size());
		});

		doInJPA(entityManager -> {
			//That would not work without @Loader(namedQuery = "findTagById")
			assertNull(entityManager.find(Tag.class, _miscTag.getId()));
		});

		doInJPA(entityManager -> {
			Boolean exists = entityManager.createQuery("""
				select count(t) = 1
				from Tag t
				where t.name = :name
				""", Boolean.class)
			.setParameter("name", "Misc")
			.getSingleResult();

			assertFalse(exists);
		});

		doInJPA(entityManager -> {
			List<Tag> tags = entityManager.createQuery("select t from Tag t", Tag.class).getResultList();
			//That would not work without @Where(clause = "deleted = false")
			assertEquals(3, tags.size());
		});
	}

	@Test
	public void testRemovePostDetails() {
		doInJPA(entityManager -> {
			Post post = new Post()
				.setId(1L)
				.setTitle("High-Performance Java Persistence");

			post.addDetails(
				new PostDetails()
					.setCreatedOn(
						Timestamp.valueOf(LocalDateTime.of(2023, 7, 20, 12, 0, 0))
					)
			);

			entityManager.persist(post);
		});

		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertNotNull(post.getDetails());

			post.removeDetails();
		});

		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertNull(post.getDetails());
		});

		doInJPA(entityManager -> {
			assertNull(entityManager.find(PostDetails.class, 1L));
		});
	}

	@Test
	public void testRemovePostComment() {
		doInJPA(entityManager -> {
			entityManager.persist(
				new Post()
					.setId(1L)
					.setTitle("High-Performance Java Persistence")
					.addComment(
						new PostComment()
							.setId(1L)
							.setReview("Great!")
					)
					.addComment(
						new PostComment()
							.setId(2L)
							.setReview("To read")
					)
			);
		});

		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(2, post.getComments().size());

			assertNotNull(entityManager.find(PostComment.class, 2L));

			post.removeComment(post.getComments().get(1));
		});

		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(1, post.getComments().size());
			assertNull(entityManager.find(PostComment.class, 2L));
		});
	}

	@Test
	public void testRemovePost() {
		doInJPA(entityManager -> {
			Session session = entityManager.unwrap(Session.class);

			entityManager.persist(
				new Post()
					.setId(1L)
					.setTitle("High-Performance Java Persistence")
					.addDetails(
						new PostDetails()
							.setCreatedOn(
								Timestamp.valueOf(
									LocalDateTime.of(2023, 7, 20, 12, 0, 0)
								)
							)
					)
					.addTag(session.bySimpleNaturalId(Tag.class).getReference("Java"))
					.addTag(session.bySimpleNaturalId(Tag.class).getReference("Hibernate"))
					.addTag(session.bySimpleNaturalId(Tag.class).getReference("Misc"))
					.addComment(
						new PostComment()
							.setId(1L)
							.setReview("Great!")
					)
					.addComment(
						new PostComment()
							.setId(2L)
							.setReview("To read")
					)
			);
		});

		doInJPA(entityManager -> {
			Post post = entityManager.createQuery("""
       			select p
       			from Post p
       			join fetch p.comments
       			join fetch p.details
       			where p.id = :id
   				""", Post.class)
			.setParameter("id", 1L)
			.getSingleResult();

			entityManager.remove(post);
		});
	}

	@Test
	public void testRemoveAndFindPostComment() {
		doInJPA(entityManager -> {
			Post post = new Post()
				.setId(1L)
				.setTitle("High-Performance Java Persistence");
			entityManager.persist(post);

			post.addComment(
				new PostComment()
					.setId(1L)
					.setReview("Great!")
			);

			post.addComment(
				new PostComment()
					.setId(2L)
					.setReview("Excellent!")
			);
		});
		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			post.removeComment(post.getComments().get(0));
		});
		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(1, post.getComments().size());
		});
	}

	@Entity(name = "Post")
	@Table(name = "post")
	@SoftDelete
	public static class Post {

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
		@SoftDelete
		private List<Tag> tags = new ArrayList<>();

		@Version
		private short version;

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

		public List<PostComment> getComments() {
			return comments;
		}

		public PostDetails getDetails() {
			return details;
		}

		public List<Tag> getTags() {
			return tags;
		}

		public Post addComment(PostComment comment) {
			comments.add(comment);
			comment.setPost(this);
			return this;
		}

		public Post removeComment(PostComment comment) {
			comments.remove(comment);
			comment.setPost(null);
			return this;
		}

		public Post addDetails(PostDetails details) {
			this.details = details;
			details.setPost(this);
			return this;
		}

		public Post removeDetails() {
			this.details.setPost(null);
			this.details = null;
			return this;
		}

		public Post addTag(Tag tag) {
			tags.add(tag);
			return this;
		}
	}

	@Entity(name = "PostDetails")
	@Table(name = "post_details")
	@SoftDelete
	public static class PostDetails {

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
		@NotFound(action = NotFoundAction.EXCEPTION)
		private Post post;

		@Version
		private short version;

		public Long getId() {
			return id;
		}

		public PostDetails setId(Long id) {
			this.id = id;
			return this;
		}

		public Post getPost() {
			return post;
		}

		public PostDetails setPost(Post post) {
			this.post = post;
			return this;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public PostDetails setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
			return this;
		}

		public String getCreatedBy() {
			return createdBy;
		}

		public PostDetails setCreatedBy(String createdBy) {
			this.createdBy = createdBy;
			return this;
		}
	}

	@Entity(name = "PostComment")
	@Table(name = "post_comment")
	@SoftDelete
	public static class PostComment {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		private Post post;

		private String review;

		@Version
		private short version;

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
	}

	@Entity(name = "Tag")
	@Table(name = "tag")
	@SoftDelete
	public static class Tag {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String name;

		@Version
		private short version;

		public Long getId() {
			return id;
		}

		public Tag setId(Long id) {
			this.id = id;
			return this;
		}

		public String getName() {
			return name;
		}

		public Tag setName(String name) {
			this.name = name;
			return this;
		}
	}
}
