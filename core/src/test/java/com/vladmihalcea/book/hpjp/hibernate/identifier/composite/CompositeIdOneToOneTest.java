package com.vladmihalcea.book.hpjp.hibernate.identifier.composite;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;

public class CompositeIdOneToOneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Employee.class,
            EmployeeDetails.class,
        };
    }

    @Test
    public void test() {
        LOGGER.debug("test");

        doInJPA(entityManager -> {
            Employee employee = new Employee();
            employee.setId(new EmployeeId(1L, 100L));
            employee.setName("Vlad Mihalcea");
            entityManager.persist(employee);
        });
        doInJPA(entityManager -> {
            Employee employee = entityManager.find(Employee.class, new EmployeeId(1L, 100L));
            EmployeeDetails employeeDetails = new EmployeeDetails();
            employeeDetails.setEmployee(employee);
            employeeDetails.setDetails("High-Performance Java Persistence");
            entityManager.persist(employeeDetails);
        });
        doInJPA(entityManager -> {
            EmployeeDetails employeeDetails = entityManager.find(EmployeeDetails.class, new EmployeeId(1L, 100L));
            assertNotNull(employeeDetails);
        });
        doInJPA(entityManager -> {
            Employee employee = entityManager.find(Employee.class, new EmployeeId(1L, 100L));
            assertNotNull(employee.getDetails());
        });
    }

    @Entity(name = "Employee")
    public static class Employee {

        @EmbeddedId
        private EmployeeId id;

        private String name;

        @OneToOne(mappedBy = "employee")
        private EmployeeDetails details;

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

        public EmployeeDetails getDetails() {
            return details;
        }

        public void setDetails(EmployeeDetails details) {
            this.details = details;
        }
    }

    @Entity(name = "EmployeeDetails")
    public static class EmployeeDetails {

        @EmbeddedId
        private EmployeeId id;

        @MapsId
        @OneToOne
        private Employee employee;

        private String details;

        public EmployeeId getId() {
            return id;
        }

        public void setId(EmployeeId id) {
            this.id = id;
        }

        public Employee getEmployee() {
            return employee;
        }

        public void setEmployee(Employee employee) {
            this.employee = employee;
            this.id = employee.getId();
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }

    @Embeddable
    public static class EmployeeId implements Serializable {

        private Long companyId;

        private Long employeeId;

        public EmployeeId() {
        }

        public EmployeeId(Long companyId, Long employeeId) {
            this.companyId = companyId;
            this.employeeId = employeeId;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmployeeId)) return false;
            EmployeeId that = (EmployeeId) o;
            return Objects.equals(getCompanyId(), that.getCompanyId()) &&
                    Objects.equals(getEmployeeId(), that.getEmployeeId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getCompanyId(), getEmployeeId());
        }
    }

}
