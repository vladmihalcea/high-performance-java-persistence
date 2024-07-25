package com.vladmihalcea.hpjp.hibernate.mapping.softdelete;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.StaleStateException;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
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
public class SoftDeleteVersionTest extends AbstractTest {

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
			entityManager.persist(new Tag().setName("Java"));
			entityManager.persist(new Tag().setName("JPA"));
			entityManager.persist(new Tag().setName("Hibernate"));
			entityManager.persist(new Tag().setName("Misc"));
		} );
	}

	@Test
	public void testRemoveTag() {
		doInJPA(entityManager -> {
			Post post = new Post()
				.setTitle("High-Performance Java Persistence");

			entityManager.persist(post);

			Session session = entityManager.unwrap(Session.class);

			post.addTag(session.bySimpleNaturalId(Tag.class).getReference("Java"));
			post.addTag(session.bySimpleNaturalId(Tag.class).getReference("Hibernate"));
			post.addTag(session.bySimpleNaturalId(Tag.class).getReference("Misc"));
		});

		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(3, post.getTags().size());
		});

		Tag miscTag = doInJPA(entityManager -> {
			return entityManager.unwrap(Session.class).bySimpleNaturalId(Tag.class).getReference("Misc");
		});

		doInJPA(entityManager -> {
			entityManager.remove(miscTag);
		});

		doInJPA(entityManager -> {
			LOGGER.info("Load post entity");
			Post post = entityManager.find(Post.class, 1L);
			LOGGER.info("Load associated Tag entities");
			assertEquals(2, post.getTags().size());
		});

		doInJPA(entityManager -> {
			//That would not work without @Loader(namedQuery = "findTagById")
			assertNull(entityManager.find(Tag.class, miscTag.getId()));

			assertEquals(
				miscTag.getName(),
				entityManager.createNativeQuery("""
					SELECT 
						name
					FROM 
						tag
					WHERE
						id = :id  
					""")
				.setParameter("id", miscTag.getId())
				.getSingleResult()
			);
		});

		doInJPA(entityManager -> {
			List<Tag> tags = entityManager.createQuery("""
   				select t
   				from Tag t
   				""", Tag.class)
				.getResultList();
			//That would not work without @Where(clause = "deleted = false")
			assertEquals(3, tags.size());
		});
	}

	@Test
	public void testRemovePostDetails() {
		doInJPA(entityManager -> {
			entityManager.persist(
				new Post()
					.setTitle("High-Performance Java Persistence")
					.addDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"))
					.addComment(new PostComment().setReview("Excellent!"))
					.addComment(new PostComment().setReview("Great!"))
			);
		});

		doInJPA(entityManager -> {
			Post post = entityManager.createQuery("""
       			select p
       			from Post p
       			join fetch p.details
       			where p.title = :title	
   				""", Post.class)
			.setParameter("title", "High-Performance Java Persistence")
			.getSingleResult();

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
					.setTitle("High-Performance Java Persistence")
					.addDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"))
					.addComment(new PostComment().setReview("Excellent!"))
					.addComment(new PostComment().setReview("Great!"))
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
	public void testRemoveAndFindPostComment() {
		doInJPA(entityManager -> {
			entityManager.persist(
				new Post()
					.setTitle("High-Performance Java Persistence")
					.addDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"))
					.addComment(new PostComment().setReview("Excellent!"))
					.addComment(new PostComment().setReview("Great!"))
			);
		});
		
		doInJPA(entityManager -> {
			Post post = entityManager.createQuery("""
       			select p
       			from Post p
       			left join fetch p.comments
       			where p.title = :title	
   				""", Post.class)
			.setParameter("title", "High-Performance Java Persistence")
			.getSingleResult();

			post.removeComment(post.getComments().get(0));
		});
		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertEquals(1, post.getComments().size());
		});
	}

	@Test
	public void testRemovePost() {
		doInJPA(entityManager -> {
			Session session = entityManager.unwrap(Session.class);

			entityManager.persist(
				new Post()
					.setTitle("High-Performance Java Persistence")
					.addDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"))
					.addComment(new PostComment().setReview("Excellent!"))
					.addComment(new PostComment().setReview("Great!"))
			);
		});

		doInJPA(entityManager -> {
			Post post = entityManager.createQuery("""
       			select p
       			from Post p
       			left join fetch p.details
       			left join fetch p.comments
       			where p.title = :title	
   				""", Post.class)
			.setParameter("title", "High-Performance Java Persistence")
			.getSingleResult();

			entityManager.remove(post);
		});
		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertNull(post);
		});
	}

	@Test
	public void testConcurrentUpdate() {
		Post post_ = new Post().setTitle("High-Performance Java Persistence");
		doInJPA(entityManager -> {
			entityManager.persist(post_);
		});

		try {
			doInJPA(entityManager -> {
				Post post = entityManager.find(Post.class, post_.id);

				executeSync(() -> {
					doInJPA(_entityManager -> {
						_entityManager.remove(_entityManager.find(Post.class, post_.id));
					});
				});

				post.setTitle("High-Performance Java Persistence, 2nd edition");
			});
		} catch (Exception e) {
			assertTrue(StaleStateException.class.isAssignableFrom(ExceptionUtil.rootCause(e).getClass()));
		}

		doInJPA(entityManager -> {
			Post post = entityManager.find(Post.class, 1L);
			assertNull(post);
		});
	}

	@Entity(name = "Post")
	@Table(name = "post")
	@SQLDelete(sql = """
		UPDATE post
		SET 
			deleted = true,
			version = version + 1
		WHERE
			id = ? AND
			version = ?
		""")
	@Loader(namedQuery = "findPostById")
	@NamedQuery(name = "findPostById", query = """
		select p
		from Post p
		where
			p.id = ?1 and
			p.deleted = false
		""")
	@Where(clause = "deleted = false")
	public static class Post extends SoftDeletable {

		@Id
		@GeneratedValue
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

		@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		@JoinTable(
			name = "post_tag",
			joinColumns = @JoinColumn(name = "post_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id")
		)
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
	@SQLDelete(sql = """
		UPDATE post_details
		SET 
			deleted = true,
			version = version + 1
		WHERE
			id = ? AND
			version = ?
		""")
	@Loader(namedQuery = "findPostDetailsById")
	@NamedQuery(name = "findPostDetailsById", query = """
		select pd
		from PostDetails pd
		where
			pd.id = ?1 and
			pd.deleted = false
		""")
	@Where(clause = "deleted = false")
	public static class PostDetails extends SoftDeletable {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "created_on")
		private Date createdOn = new Date();

		@Column(name = "created_by")
		private String createdBy;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "id")
		@MapsId
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
	@SQLDelete(sql = """
		UPDATE post_comment
		SET 
			deleted = true,
			version = version + 1
		WHERE
			id = ? AND
			version = ?
		""")
	@Loader(namedQuery = "findPostCommentById")
	@NamedQuery(name = "findPostCommentById", query = """
		select pc
		from PostComment pc
		where
			pc.id = ?1 and
			pc.deleted = false
		""")
	@Where(clause = "deleted = false")
	public static class PostComment extends SoftDeletable {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
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
	@SQLDelete(sql = """
		UPDATE tag
		SET 
			deleted = true,
			version = version + 1
		WHERE
			id = ? AND
			version = ?
		""")
	@Loader(namedQuery = "findTagById")
	@NamedQuery(name = "findTagById", query = """
		select t
		from Tag t
		where
			t.id = ?1 and
			t.deleted = false
		""")
	@Where(clause = "deleted = false")
	public static class Tag extends SoftDeletable {

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

	@MappedSuperclass
	public static abstract class SoftDeletable {

		private boolean deleted;

		public boolean isDeleted() {
			return deleted;
		}
	}
}
