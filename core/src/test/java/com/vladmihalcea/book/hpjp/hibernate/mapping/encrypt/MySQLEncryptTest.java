package com.vladmihalcea.book.hpjp.hibernate.mapping.encrypt;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import org.hibernate.Session;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.Test;

import jakarta.persistence.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MySQLEncryptTest extends AbstractTest {

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
			setEncryptionKey(entityManager);

			User user = new User()
				.setId(1L)
				.setUsername("vladmihalcea");

			entityManager.persist(user);

			entityManager.persist(
				new UserDetails()
					.setUser(user)
					.setFirstName("Vlad")
					.setLastName("Mihalcea")
					.setEmailAddress("vlad@vladmihalcea.com")
			);
		});

		doInJPA(entityManager -> {
			setEncryptionKey(entityManager);
			
			UserDetails userDetails = entityManager.find(
				UserDetails.class,
				1L
			);

			assertEquals("Vlad", userDetails.getFirstName());
			assertEquals("Mihalcea", userDetails.getLastName());
			assertEquals("vlad@vladmihalcea.com", userDetails.getEmailAddress());
		});
	}

	private void setEncryptionKey(EntityManager entityManager) {
		Session session = entityManager.unwrap(Session.class);
		Dialect dialect = session.getSessionFactory().unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
		String encryptionKey = ReflectionUtils.invokeMethod(
			dialect,
			"inlineLiteral",
			"encryptionKey"
		);

		session.doWork(connection -> {
			update(
				connection,
				String.format(
					"SET @encryption_key = '%s'", encryptionKey
				)
			);
		});
	}

	@Entity
	@Table(name = "users")
	public static class User {

		@Id
		private Long id;

		private String username;

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
	}

	@Entity
	@Table(name = "user_details")
	public static class UserDetails {

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY)
		@MapsId
		@JoinColumn(name = "id")
		private User user;

		@ColumnTransformer(
			read = "AES_DECRYPT(first_name, @encryption_key)",
			write = "AES_ENCRYPT(?, @encryption_key)"
		)
		@Column(name = "first_name", columnDefinition = "VARBINARY(100)")
		private String firstName;

		@ColumnTransformer(
			read = "AES_DECRYPT(last_name, @encryption_key)",
			write = "AES_ENCRYPT(?, @encryption_key)"
		)
		@Column(name = "last_name", columnDefinition = "VARBINARY(100)")
		private String lastName;

		@ColumnTransformer(
			read = "AES_DECRYPT(email_address, @encryption_key)",
			write = "AES_ENCRYPT(?, @encryption_key)"
		)
		@Column(name = "email_address", columnDefinition = "VARBINARY(100)")
		private String emailAddress;

		public Long getId() {
			return id;
		}

		public UserDetails setId(Long id) {
			this.id = id;
			return this;
		}

		public User getUser() {
			return user;
		}

		public UserDetails setUser(User user) {
			this.user = user;
			this.id = user.getId();
			return this;
		}

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
