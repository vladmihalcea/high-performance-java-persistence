package com.vladmihalcea.book.hpjp.hibernate.identifier.composite;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CompositeIdManyToOneTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Company.class,
            Employee.class,
            Phone.class,
        };
    }

    @Test
    public void test() {
        LOGGER.debug("test");

        doInJPA(entityManager -> {
            Company company = new Company();
            company.setId(1L);
            company.setName("vladmihalcea.com");
            entityManager.persist(company);

            Employee employee = new Employee();
            employee.setId(new EmployeeId(1L, 100L));
            employee.setName("Vlad Mihalcea");
            entityManager.persist(employee);
        });

        doInJPA(entityManager -> {
            Employee employee = entityManager.find(Employee.class, new EmployeeId(1L, 100L));
            Phone phone = new Phone();
            phone.setEmployee(employee);
            phone.setNumber("012-345-6789");
            entityManager.persist(phone);
        });

        doInJPA(entityManager -> {
            Phone phone = entityManager.find(Phone.class, "012-345-6789");
            assertNotNull(phone);
            assertEquals(new EmployeeId(1L, 100L), phone.getEmployee().getId());
        });

        doInJPA(entityManager -> {
            List<Employee> employees = entityManager
            .createQuery(
                "select e " +
                "from Employee e " +
                "where e.id.companyId = :companyId")
            .setParameter("companyId", 1L)
            .getResultList();

            assertEquals(new EmployeeId(1L, 100L), employees.get(0).getId());
        });
    }

    @Entity(name = "Company")
    @Table(name = "company")
    public static class Company {

        @Id
        private Long id;

        private String name;

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
    }

    @Entity(name = "Employee")
    @Table(name = "employee")
    public static class Employee {

        @EmbeddedId
        private EmployeeId id;

        private String name;

        @ManyToOne
        @JoinColumn(name = "company_id",insertable = false, updatable = false)
        private Company company;

        public EmployeeId getId() {
            return id;
        }

        public void setId(EmployeeId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity(name = "Phone")
    @Table(name = "phone")
    public static class Phone {

        @Id
        @Column(name = "`number`")
        private String number;

        @ManyToOne
        @JoinColumns({
            @JoinColumn(
                name = "company_id",
                referencedColumnName = "company_id"),
            @JoinColumn(
                name = "employee_number",
                referencedColumnName = "employee_number")
        })
        private Employee employee;

        public Employee getEmployee() {
            return employee;
        }

        public void setEmployee(Employee employee) {
            this.employee = employee;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }
    }

    @Embeddable
    public static class EmployeeId implements Serializable {

        @Column(name = "company_id")
        private Long companyId;

        @Column(name = "employee_number")
        private Long employeeNumber;

        public EmployeeId() {
        }

        public EmployeeId(Long companyId, Long employeeId) {
            this.companyId = companyId;
            this.employeeNumber = employeeId;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public Long getEmployeeNumber() {
            return employeeNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmployeeId)) return false;
            EmployeeId that = (EmployeeId) o;
            return Objects.equals(getCompanyId(), that.getCompanyId()) &&
                    Objects.equals(getEmployeeNumber(), that.getEmployeeNumber());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getCompanyId(), getEmployeeNumber());
        }
    }

}
