package com.vladmihalcea.book.hpjp.hibernate.mapping.encrypt;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.CryptoUtils;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MySQLJsonEncryptTest extends AbstractTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			User.class,
			UserDetails.class,
		};
	}

	@Override
	protected Database database() {
		return Database.MYSQL;
	}

	@Test
	public void test() {
		doInJPA(entityManager -> {
			User user = new User()
				.setId(1L)
				.setUsername("vladmihalcea")
				.setDetails(
					new UserDetails()
					.setFirstName("Vlad")
					.setLastName("Mihalcea")
					.setEmailAddress("info@vladmihalcea.com")
				);

			entityManager.persist(user);
		});

		doInJPA(entityManager -> {
			User user = entityManager.find(
				User.class,
				1L
			);

			UserDetails userDetails = user.getDetails();

			assertEquals("Vlad", userDetails.getFirstName());
			assertEquals("Mihalcea", userDetails.getLastName());
			assertEquals("info@vladmihalcea.com", userDetails.getEmailAddress());
		});

		doInJPA(entityManager -> {
			User user = entityManager.find(User.class, 1L);

			user.getDetails().setEmailAddress("noreply@vladmihalcea.com");
		});
	}

	@Entity
	@Table(name = "users")
	@DynamicUpdate
	public static class User {

		@Id
		private Long id;

		private String username;

		@Type(JsonStringType.class)
		@Column(columnDefinition = "json")
		private UserDetails details;

		public Long getId() {
			return id;
		}

		public User setId(Long id) {
			this.id = id;
			return this;
		}

		public String getUsername() {
			return username;
		}

		public User setUsername(String username) {
			this.username = username;
			return this;
		}

		public UserDetails getDetails() {
			return details;
		}

		public User setDetails(UserDetails details) {
			this.details = details;
			return this;
		}

		@PrePersist
		@PreUpdate
		private void encryptFields() {
			if (details != null) {
				if (details.getFirstName() != null) {
					details.setFirstName(
						CryptoUtils.encrypt(details.getFirstName())
					);
				}
				if (details.getLastName() != null) {
					details.setLastName(
						CryptoUtils.encrypt(details.getLastName())
					);
				}
				if (details.getEmailAddress() != null) {
					details.setEmailAddress(
						CryptoUtils.encrypt(details.getEmailAddress())
					);
				}
			}
		}

		@PostLoad
		private void decryptFields() {
			if (details != null) {
				if (details.getFirstName() != null) {
					details.setFirstName(
						CryptoUtils.decrypt(details.getFirstName())
					);
				}
				if (details.getLastName() != null) {
					details.setLastName(
						CryptoUtils.decrypt(details.getLastName())
					);
				}
				if (details.getEmailAddress() != null) {
					details.setEmailAddress(
						CryptoUtils.decrypt(details.getEmailAddress())
					);
				}
			}
		}
	}

	public static class UserDetails {

		private String firstName;

		private String lastName;

		private String emailAddress;

		public String getFirstName() {
			return firstName;
		}

		public UserDetails setFirstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		public String getLastName() {
			return lastName;
		}

		public UserDetails setLastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public String getEmailAddress() {
			return emailAddress;
		}

		public UserDetails setEmailAddress(String emailAddress) {
			this.emailAddress = emailAddress;
			return this;
		}
	}
}
