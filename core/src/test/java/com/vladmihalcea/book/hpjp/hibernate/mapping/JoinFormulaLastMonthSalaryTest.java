package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.JoinFormula;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JoinFormulaLastMonthSalaryTest extends AbstractPostgreSQLIntegrationTest {

	@Override
	protected Class<?>[] entities() {
		return new Class<?>[] {
			Employee.class,
			Salary.class
		};
	}

	@Override
	protected void afterInit() {
		Employee alice = new Employee();
		alice.setId(1L);
		alice.setName("Alice");
		alice.setTitle("CEO");

		Employee bob = new Employee();
		bob.setId(2L);
		bob.setName("Bob");
		bob.setTitle("Developer");

		doInJPA( entityManager -> {
			entityManager.persist(alice);
			entityManager.persist(bob);
		} );

		doInJPA( entityManager -> {
			Salary aliceSalary201511 = new Salary();
			aliceSalary201511.setId(1L);
			aliceSalary201511.setEmployee(alice);
			aliceSalary201511.setYear(2015);
			aliceSalary201511.setMonth(11);
			aliceSalary201511.setAmountCents(10_000);

			entityManager.persist(aliceSalary201511);

			Salary bobSalary201511 = new Salary();
			bobSalary201511.setId(2L);
			bobSalary201511.setEmployee(bob);
			bobSalary201511.setYear(2015);
			bobSalary201511.setMonth(11);
			bobSalary201511.setAmountCents(7_000);

			entityManager.persist(bobSalary201511);

			Salary aliceSalary201512 = new Salary();
			aliceSalary201512.setId(3L);
			aliceSalary201512.setEmployee(alice);
			aliceSalary201512.setYear(2015);
			aliceSalary201512.setMonth(12);
			aliceSalary201512.setAmountCents(11_000);

			entityManager.persist(aliceSalary201512);

			Salary bobSalary201512 = new Salary();
			bobSalary201512.setId(4L);
			bobSalary201512.setEmployee(bob);
			bobSalary201512.setYear(2015);
			bobSalary201512.setMonth(12);
			bobSalary201512.setAmountCents(7_500);

			entityManager.persist(bobSalary201512);

			Salary aliceSalary201601 = new Salary();
			aliceSalary201601.setId(5L);
			aliceSalary201601.setEmployee(alice);
			aliceSalary201601.setYear(2016);
			aliceSalary201601.setMonth(1);
			aliceSalary201601.setAmountCents(11_500);

			entityManager.persist(aliceSalary201601);

			Salary bobSalary201601 = new Salary();
			bobSalary201601.setId(6L);
			bobSalary201601.setEmployee(bob);
			bobSalary201601.setYear(2016);
			bobSalary201601.setMonth(1);
			bobSalary201601.setAmountCents(7_900);

			entityManager.persist(bobSalary201601);

			Salary aliceSalary201602 = new Salary();
			aliceSalary201602.setId(7L);
			aliceSalary201602.setEmployee(alice);
			aliceSalary201602.setYear(2016);
			aliceSalary201602.setMonth(2);
			aliceSalary201602.setAmountCents(11_900);

			entityManager.persist(aliceSalary201602);

			Salary bobSalary201602 = new Salary();
			bobSalary201602.setId(8L);
			bobSalary201602.setEmployee(bob);
			bobSalary201602.setYear(2016);
			bobSalary201602.setMonth(2);
			bobSalary201602.setAmountCents(8_500);

			entityManager.persist(bobSalary201602);
		} );

		assertEquals(Long.valueOf(1L), getPreviousSalaryId(3L));
		assertEquals(Long.valueOf(2L), getPreviousSalaryId(4L));
		assertEquals(Long.valueOf(3L), getPreviousSalaryId(5L));
		assertEquals(Long.valueOf(4L), getPreviousSalaryId(6L));
		assertEquals(Long.valueOf(5L), getPreviousSalaryId(7L));
		assertEquals(Long.valueOf(6L), getPreviousSalaryId(8L));
	}

	@Test
	public void test() {
		doInJPA( entityManager -> {
			assertEquals(
				Long.valueOf(1L),
				entityManager.find(Salary.class, 3L)
					.getPreviousMonthSalary().getId()
			);

			assertEquals(
				Long.valueOf(2L),
				entityManager.find(Salary.class, 4L)
					.getPreviousMonthSalary().getId()
			);

			assertEquals(
				Long.valueOf(3L),
				entityManager.find(Salary.class, 5L)
					.getPreviousMonthSalary().getId()
			);

			assertEquals(
				Long.valueOf(4L),
				entityManager.find(Salary.class, 6L)
					.getPreviousMonthSalary().getId()
			);

			assertEquals(
				Long.valueOf(5L),
				entityManager.find(Salary.class, 7L)
					.getPreviousMonthSalary().getId()
			);

			assertEquals(
				Long.valueOf(6L),
				entityManager.find(Salary.class, 8L)
					.getPreviousMonthSalary().getId()
			);
		} );
	}

	private Long getPreviousSalaryId(long salaryId) {
		return doInJPA( entityManager -> {
			Salary salary = entityManager.find(Salary.class, salaryId);

			Number prevSalaryId = (Number) entityManager.createNativeQuery(
				"SELECT prev_salary.id " +
				"FROM salary prev_salary " +
				"WHERE " +
				"	prev_salary.employee_id = :employeeId AND " +
				"	( CASE WHEN :month = 1 " +
				"		THEN prev_salary.year + 1 = :year AND " +
				"			 prev_salary.month = 12 " +
				"		ELSE prev_salary.year = :year AND " +
				"			 prev_salary.month + 1 = :month " +
				"	END ) = true ")
			.setParameter("employeeId", salary.getEmployee().getId())
			.setParameter("year", salary.getYear())
			.setParameter("month", salary.getMonth())
			.getSingleResult();

			return prevSalaryId.longValue();
		} );
	}

	@Entity(name = "Employee")
	@Table(name = "employee")
	public static class Employee {

		@Id
		private Long id;

		private String name;

		private String title;

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

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@Entity(name = "Salary")
	@Table(name = "salary")
	public static class Salary {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Employee employee;

		private int month;

		private int year;

		@Column(name = "amount_cents")
		private long amountCents;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinFormula(
			"( " +
			"	SELECT prev_salary.id " +
			"	FROM salary prev_salary " +
			"	WHERE " +
			"		prev_salary.employee_id = employee_id AND " +
			"		( " +
			"			CASE WHEN month = 1 " +
			"			THEN prev_salary.year + 1 = year AND " +
			"			 	 prev_salary.month = 12 " +
			"			ELSE prev_salary.year = year AND " +
			"			 	 prev_salary.month + 1 = month " +
			"			END " +
			"		) = true " +
			")"
		)
		private Salary previousMonthSalary;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Employee getEmployee() {
			return employee;
		}

		public void setEmployee(Employee employee) {
			this.employee = employee;
		}

		public int getMonth() {
			return month;
		}

		public void setMonth(int month) {
			this.month = month;
		}

		public int getYear() {
			return year;
		}

		public void setYear(int year) {
			this.year = year;
		}

		public long getAmountCents() {
			return amountCents;
		}

		public void setAmountCents(long amountCents) {
			this.amountCents = amountCents;
		}

		public Salary getPreviousMonthSalary() {
			return previousMonthSalary;
		}
	}


}
