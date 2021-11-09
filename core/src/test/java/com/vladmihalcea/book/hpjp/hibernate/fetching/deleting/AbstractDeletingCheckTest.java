package com.vladmihalcea.book.hpjp.hibernate.fetching.deleting;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.transaction.JPATransactionVoidFunction;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class AbstractDeletingCheckTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Audience.class,
            Group.class,
            Lesson.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    protected void doInJPA(JPATransactionVoidFunction function, EntityManager entityManager) {
        EntityTransaction txn = null;
        try {
            function.beforeTransactionCompletion();
            txn = entityManager.getTransaction();
            txn.begin();
            function.accept(entityManager);
            if ( !txn.getRollbackOnly() ) {
                txn.commit();
            }
            else {
                try {
                    txn.rollback();
                }
                catch (Exception e) {
                    LOGGER.error( "Rollback failure", e );
                }
            }
        } catch (Throwable t) {
            if ( txn != null && txn.isActive() ) {
                try {
                    txn.rollback();
                }
                catch (Exception e) {
                    LOGGER.error( "Rollback failure", e );
                }
            }
            throw t;
        } finally {
            function.afterTransactionCompletion();
        }
    }

    protected void createTables() {
        executeStatement("DROP TABLE IF EXISTS lessons");
        executeStatement("DROP TABLE IF EXISTS audiences");
        executeStatement("DROP TABLE IF EXISTS groups");

        executeStatement("""
            CREATE TABLE groups(
                id   BIGINT PRIMARY KEY,
                name VARCHAR
            )
            """);
        executeStatement("""
            CREATE TABLE audiences(
                id     BIGINT PRIMARY KEY,
                number INTEGER
            )
            """);
    }

    protected void insertData() {
        executeStatement("INSERT INTO groups(id, name) VALUES (1, '1')");
        executeStatement("INSERT INTO audiences(id, number) VALUES (1, 1), (2, 2)");
        executeStatement("INSERT INTO lessons(id, audience_id, group_id) VALUES (1, 1, 1)");
    }

    @Entity(name = "Audience")
    @Table(name = "audiences")
    public static class Audience {

        @Id
        private Long id;

        @Column
        private Integer number;

        @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "audience")
        private List<Lesson> lessons = new ArrayList<>();

        public Audience() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

        public List<Lesson> getLessons() {
            return lessons;
        }

        public void setLessons(List<Lesson> lessons) {
            this.lessons = lessons;
        }
    }

    @Entity(name = "Group")
    @Table(name = "groups")
    public static class Group {

        @Id
        private Long id;

        @Column
        private String name;

        @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "group")
        private List<Lesson> lessons = new ArrayList<>();

        public Group() {}

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

        public List<Lesson> getLessons() {
            return lessons;
        }

        public void setLessons(List<Lesson> lessons) {
            this.lessons = lessons;
        }
    }

    @Entity(name = "Lesson")
    @Table(name = "lessons")
    public static class Lesson {

        @Id
        private Long id;

        @ManyToOne
        @JoinColumn(name = "audience_id")
        private Audience audience;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "group_id")
        private Group group;

        public Lesson() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Audience getAudience() {
            return audience;
        }

        public void setAudience(Audience audience) {
            this.audience = audience;
        }

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            this.group = group;
        }
    }
}
