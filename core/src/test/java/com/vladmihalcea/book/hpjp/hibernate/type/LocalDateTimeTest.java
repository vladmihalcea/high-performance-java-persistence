package com.vladmihalcea.book.hpjp.hibernate.type;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class LocalDateTimeTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Employee.class,
			Meeting.class
		};
	}

	@Test
	public void testLocalDateEvent() {
		doInJPA( entityManager -> {
			Employee employee = new Employee();
			employee.setName( "Vlad Mihalcea" );
			employee.setBirthday(
				LocalDate.of(
					1981, 12, 10
				)
			);
			employee.setUpdatedOn(
				LocalDateTime.of(
					2015, 12, 1,
					8, 0, 0
				)
			);

			entityManager.persist( employee );

			Meeting meeting = new Meeting();
			meeting.setId( 1L );
			meeting.setCreatedBy( employee );
			meeting.setStartsAt(
				ZonedDateTime.of(
					2017, 6, 25,
					11, 30, 0, 0,
					ZoneId.systemDefault()
				)
			);
			meeting.setDuration(
				Duration.of( 45, ChronoUnit.MINUTES )
			);

			entityManager.persist( meeting );
		} );

		doInJPA( entityManager -> {
			Employee employee = entityManager
					.unwrap( Session.class )
					.bySimpleNaturalId( Employee.class )
					.load( "Vlad Mihalcea" );
			assertEquals(
				LocalDate.of(
					1981, 12, 10
				),
				employee.getBirthday()
			);
			assertEquals(
				LocalDateTime.of(
					2015, 12, 1,
					8, 0, 0
				),
				employee.getUpdatedOn()
			);

			Meeting meeting = entityManager.find( Meeting.class, 1L );
			assertSame(
				employee, meeting.getCreatedBy()
			);
			assertEquals(
				ZonedDateTime.of(
					2017, 6, 25,
					11, 30, 0, 0,
					ZoneId.systemDefault()
				),
				meeting.getStartsAt()
			);
			assertEquals(
				Duration.of( 45, ChronoUnit.MINUTES ),
				meeting.getDuration()
			);
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String name;

		private LocalDate birthday;

		@Column(name = "updated_on")
		private LocalDateTime updatedOn;

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

		public LocalDate getBirthday() {
			return birthday;
		}

		public void setBirthday(LocalDate birthday) {
			this.birthday = birthday;
		}

		public LocalDateTime getUpdatedOn() {
			return updatedOn;
		}

		public void setUpdatedOn(LocalDateTime updatedOn) {
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
		private ZonedDateTime startsAt;

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

		public ZonedDateTime getStartsAt() {
			return startsAt;
		}

		public void setStartsAt(ZonedDateTime startsAt) {
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
