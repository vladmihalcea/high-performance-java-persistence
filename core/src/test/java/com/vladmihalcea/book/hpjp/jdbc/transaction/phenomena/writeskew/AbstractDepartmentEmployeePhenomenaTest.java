package com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena.writeskew;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public abstract class AbstractDepartmentEmployeePhenomenaTest extends AbstractTest {

    public static final String INSERT_DEPARTMENT = "insert into department (name, budget, id) values (?, ?, ?)";

    public static final String INSERT_EMPLOYEE = "insert into employee (department_id, name, salary, id) values (?, ?, ?, ?)";


    protected final String isolationLevelName;

    protected final int isolationLevel;

    protected AbstractDepartmentEmployeePhenomenaTest(String isolationLevelName, int isolationLevel) {
        this.isolationLevelName = isolationLevelName;
        this.isolationLevel = isolationLevel;
    }

    @Override
    protected Class<?>[] entities() {
        List<Class<?>> classes = new ArrayList<>(Arrays.asList(super.entities()));
        classes.add(Department.class);
        classes.add(Employee.class);
        return classes.toArray(new Class<?>[]{});
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        levels.add(new Object[]{"Read Committed", Connection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Repeatable Read", Connection.TRANSACTION_REPEATABLE_READ});
        levels.add(new Object[]{"Serializable", Connection.TRANSACTION_SERIALIZABLE});
        return levels;
    }

    protected String sumEmployeeSalarySql() {
        return "SELECT SUM(salary) FROM employee where department_id = 1";
    }

    protected String allEmployeeSalarySql() {
        return "SELECT salary FROM employee where department_id = 1";
    }

    protected String insertEmployeeSql() {
        return INSERT_EMPLOYEE;
    }

    protected String updateEmployeeSalarySql() {
        return "UPDATE employee SET salary = salary * 1.1 WHERE department_id = 1";
    }

    @Override
    public void afterInit() {
        doInJDBC(connection -> {
            try (
                    PreparedStatement departmentStatement = connection.prepareStatement(INSERT_DEPARTMENT);
                    PreparedStatement employeeStatement = connection.prepareStatement(INSERT_EMPLOYEE);
            ) {
                int index = 0;
                departmentStatement.setString(++index, "IT");
                departmentStatement.setLong(++index, 100_000);
                departmentStatement.setLong(++index, 1);
                departmentStatement.executeUpdate();

                index = 0;

                employeeStatement.setLong(++index, 1);
                employeeStatement.setString(++index, "Alice");
                employeeStatement.setLong(++index, 40_000);
                employeeStatement.setLong(++index, 1);
                employeeStatement.executeUpdate();

                index = 0;

                employeeStatement.setLong(++index, 1);
                employeeStatement.setString(++index, "Bob");
                employeeStatement.setLong(++index, 30_000);
                employeeStatement.setLong(++index, 2);
                employeeStatement.executeUpdate();

                index = 0;

                employeeStatement.setLong(++index, 1);
                employeeStatement.setString(++index, "Carol");
                employeeStatement.setLong(++index, 20_000);
                employeeStatement.setLong(++index, 3);
                employeeStatement.executeUpdate();

            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    protected void prepareConnection(Connection connection) throws SQLException {
        connection.setTransactionIsolation(isolationLevel);
        setJdbcTimeout(connection);
    }

    @Entity(name = "Department")
    @Table(name = "department")
    public static class Department {

        @Id
        private Long id;

        private String name;

        private long budget;

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

        public long getBudget() {
            return budget;
        }

        public void setBudget(long budget) {
            this.budget = budget;
        }
    }

    @Entity(name = "Employee")
    @Table(name = "employee", indexes = @Index(name = "IDX_Employee", columnList = "department_id"))
    public static class Employee {

        @Id
        private Long id;

        @Column(name = "name")
        private String name;

        @ManyToOne(fetch = FetchType.LAZY)
        private Department department;

        private long salary;

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

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public long getSalary() {
            return salary;
        }

        public void setSalary(long salary) {
            this.salary = salary;
        }
    }

}
