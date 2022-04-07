package com.vladmihalcea.book.hpjp.hibernate.mapping;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class MapsIdWithManyToOneTest extends AbstractTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Person.class,
			Group.class,
			GroupAssociationEntity.class
		};
	}

	@Test
	public void testLifecycle() {

		doInJPA( entityManager -> {

			Person person1 = new Person();
			person1.id = "abc1";
			entityManager.persist(person1);

			Person person2 = new Person();
			person2.id = "abc2";
			entityManager.persist(person2);

			Group group1 = new Group();
			group1.id = "g1";
			entityManager.persist(group1);

			Group group2 = new Group();
			group2.id = "g2";
			entityManager.persist(group2);

			GroupAssociationEntity p1g1 = new GroupAssociationEntity();
			p1g1.id = new GroupAssociationKey("g1", "abc1");
			p1g1.group = group1;
			p1g1.person = person1;
			entityManager.persist(p1g1);

			GroupAssociationEntity p2g1 = new GroupAssociationEntity();
			p2g1.id = new GroupAssociationKey( "g1", "abc2" );
			p2g1.group = group2;
			p2g1.person = person2;
			entityManager.persist(p2g1);
		} );

		doInJPA( entityManager -> {
			Group group = entityManager.find(Group.class, "g1");
			assertEquals("abc1", group.person.id.getMemberOf());
		} );

		doInJPA( entityManager -> {
			Person person = entityManager.find(Person.class, "abc2");
			assertEquals("g2", person.group.id.getId());
		} );

	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@Column(name = "id")
		private String id;


		@OneToOne(mappedBy = "person")
		private GroupAssociationEntity group;
	}

	@Entity(name = "GroupAssociationEntity")
	public static class GroupAssociationEntity {

		@EmbeddedId
		private GroupAssociationKey id;

		@OneToOne
		@MapsId("id")
		private Group group;

		@OneToOne
		@MapsId("memberOf")
		private Person person;
	}

	@Embeddable
	public static class GroupAssociationKey implements Serializable {

		private String id;

		private String memberOf;

		public GroupAssociationKey() {
		}

		public GroupAssociationKey(String id, String memberOf) {
			this.id = id;
			this.memberOf = memberOf;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getMemberOf() {
			return memberOf;
		}

		public void setMemberOf(String memberOf) {
			this.memberOf = memberOf;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof GroupAssociationKey)) return false;
			GroupAssociationKey that = (GroupAssociationKey) o;
			return Objects.equals(getId(), that.getId()) &&
					Objects.equals(getMemberOf(), that.getMemberOf());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getId(), getMemberOf());
		}
	}

	@Entity(name = "Group")
	@Table(name = "groups")
	public static class Group {

		@Id
		@Column(name = "id")
		private String id;

		@OneToOne(mappedBy = "group")
		private GroupAssociationEntity person;

	}
}
