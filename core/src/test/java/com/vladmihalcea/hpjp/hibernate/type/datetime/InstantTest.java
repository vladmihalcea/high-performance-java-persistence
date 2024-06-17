package com.vladmihalcea.hpjp.hibernate.type.datetime;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.junit.Test;

import java.time.*;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class InstantTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Employee.class,
			Meeting.class
		};
	}

	@Test
	public void test() {
		doInJPA(entityManager -> {
			Employee employee = new Employee();
			employee.setName("Vlad Mihalcea");
			employee.setBirthday(
				LocalDate.of(
					1981, 12, 10
				).atStartOfDay(ZoneOffset.UTC).toInstant()
			);
			employee.setUpdatedOn(
				LocalDateTime.of(
					2015, 12, 1,
					8, 0, 0
				).toInstant(ZoneOffset.UTC)
			);

			entityManager.persist(employee);

			Meeting meeting = new Meeting();
			meeting.setId(1L);
			meeting.setCreatedBy(employee);
			meeting.setStartsAt(
				ZonedDateTime.of(
					2017, 6, 25,
					11, 30, 0, 0,
					ZoneOffset.UTC
				).toInstant()
			);
			meeting.setDuration(
				Duration.of(45, ChronoUnit.MINUTES)
			);

			entityManager.persist(meeting);
		});

		doInJPA(entityManager -> {
			Employee employee = entityManager
				.unwrap(Session.class)
				.bySimpleNaturalId(Employee.class)
				.load("Vlad Mihalcea");
			assertEquals(
				LocalDate.of(
					1981, 12, 10
				).atStartOfDay().toInstant(ZoneOffset.UTC),
				employee.getBirthday()
			);
			assertEquals(
				LocalDateTime.of(
					2015, 12, 1,
					8, 0, 0
				).toInstant(ZoneOffset.UTC),
				employee.getUpdatedOn()
			);

			Meeting meeting = entityManager.find(Meeting.class, 1L);
			assertSame(
				employee, meeting.getCreatedBy()
			);
			assertEquals(
				ZonedDateTime.of(
					2017, 6, 25,
					11, 30, 0, 0,
					ZoneOffset.UTC
				).toInstant(),
				meeting.getStartsAt()
			);
			assertEquals(
				Duration.of(45, ChronoUnit.MINUTES),
				meeting.getDuration()
			);
		});
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String name;

		private Instant birthday;

		@Column(name = "updated_on")
		private Instant updatedOn;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Instant getBirthday() {
			return birthday;
		}

		public void setBirthday(Instant birthday) {
			this.birthday = birthday;
		}

		public Instant getUpdatedOn() {
			return updatedOn;
		}

		public void setUpdatedOn(Instant updatedOn) {
			this.updatedOn = updatedOn;
		}
	}

	@Entity(name = "Meeting")
	public static class Meeting {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "employee_id")
		private Employee createdBy;

		@Column(name = "starts_at")
		private Instant startsAt;

		@Type(PostgreSQLIntervalType.class)
		@Column(columnDefinition = "interval")
		private Duration duration;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Employee getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(Employee createdBy) {
			this.createdBy = createdBy;
		}

		public Instant getStartsAt() {
			return startsAt;
		}

		public void setStartsAt(Instant startsAt) {
			this.startsAt = startsAt;
		}

		public Duration getDuration() {
			return duration;
		}

		public void setDuration(Duration duration) {
			this.duration = duration;
		}
	}
}
