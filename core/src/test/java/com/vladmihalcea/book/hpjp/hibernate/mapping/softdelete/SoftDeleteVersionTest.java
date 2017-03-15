package com.vladmihalcea.book.hpjp.hibernate.mapping.softdelete;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class SoftDeleteVersionTest extends AbstractTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
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
			Tag miscTag = entityManager.getReference(Tag.class, "Misc");
			entityManager.remove(miscTag);
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

	@Entity(name = "Tag")
	@Table(name = "tag")
	@SQLDelete(sql =
		"UPDATE tag " +
		"SET deleted = true " +
		"WHERE id = ? and version = ?")
	@Loader(namedQuery = "findTagById")
	@NamedQuery(name = "findTagById", query =
		"SELECT t " +
		"FROM Tag t " +
		"WHERE " +
		"	t.id = ?1 AND " +
		"	t.deleted = false")
	@Where(clause = "deleted = false")
	public static class Tag extends BaseEntity {

		@Id
		private String id;

		@Version
		private int version;

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
