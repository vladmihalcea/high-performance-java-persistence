package com.vladmihalcea.book.hpjp.hibernate.identifier.composite;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CompositeIdManyToOneWithCompanyInIdTest extends AbstractTest {

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

        Company company = doInJPA(entityManager -> {
            Company _company = new Company();
            _company.setId(1L);
            _company.setName("vladmihalcea.com");
            entityManager.persist(_company);
            return _company;
        });

        doInJPA(entityManager -> {
            Employee employee = new Employee();
            employee.setId(new EmployeeId(company, 100L));
            employee.setName("Vlad Mihalcea");
            entityManager.persist(employee);
        });
        doInJPA(entityManager -> {
            Employee employee = entityManager.find(Employee.class, new EmployeeId(company, 100L));
            Phone phone = new Phone();
            phone.setEmployee(employee);
            phone.setNumber("012-345-6789");
            entityManager.persist(phone);
        });
        doInJPA(entityManager -> {
            Phone phone = entityManager.find(Phone.class, "012-345-6789");
            assertNotNull(phone);
            assertEquals(new EmployeeId(company, 100L), phone.getEmployee().getId());
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Company)) return false;
            Company company = (Company) o;
            return Objects.equals(getName(), company.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName());
        }
    }

    @Entity(name = "Employee")
    @Table(name = "employee")
    public static class Employee {

        @EmbeddedId
        private EmployeeId id;

        private String name;

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

        @ManyToOne
        @JoinColumn(name = "company_id")
        private Company company;

        @Column(name = "employee_number")
        private Long employeeNumber;

        public EmployeeId() {
        }

        public EmployeeId(Company company, Long employeeId) {
            this.company = company;
            this.employeeNumber = employeeId;
        }

        public Company getCompany() {
            return company;
        }

        public Long getEmployeeNumber() {
            return employeeNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmployeeId)) return false;
            EmployeeId that = (EmployeeId) o;
            return Objects.equals(getCompany(), that.getCompany()) &&
                    Objects.equals(getEmployeeNumber(), that.getEmployeeNumber());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getCompany(), getEmployeeNumber());
        }
    }

}
