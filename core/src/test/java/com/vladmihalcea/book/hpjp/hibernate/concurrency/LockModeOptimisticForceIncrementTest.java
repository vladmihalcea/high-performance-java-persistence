package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.annotations.Immutable;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * LockModeOptimisticForceIncrementTest - Test to check LockMode.OPTIMISTIC_FORCE_INCREMENT
 *
 * @author Carol Mihalcea
 */
public class LockModeOptimisticForceIncrementTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Repository.class,
                Commit.class
        };
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
        Repository repository = new Repository("Hibernate-Master-Class");
        entityManager.persist(repository);
        entityManager.flush();
        });
    }

    @Test
    public void testOptimisticForceIncrementLocking() throws InterruptedException {
        LOGGER.info("Test Single OPTIMISTIC_FORCE_INCREMENT Lock Mode ");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            Repository repository = (Repository) entityManager.find(Repository.class, 1L);
            session.buildLockRequest(new LockOptions(LockMode.OPTIMISTIC_FORCE_INCREMENT)).lock(repository);
            Commit commit = new Commit(repository);
            commit.getChanges().add(new Change("README.txt", "0a1,5..."));
            commit.getChanges().add(new Change("web.xml", "17c17..."));
            entityManager.persist(commit);
        });
    }

    @Test
    public void testConcurrentOptimisticForceIncrementLocking() throws InterruptedException {
        LOGGER.info("Test Concurrent OPTIMISTIC_FORCE_INCREMENT Lock Mode ");
        try {
            doInJPA(entityManager -> {
                Session session = entityManager.unwrap(Session.class);
                Repository repository = (Repository) entityManager.find(Repository.class, 1L);
                session.buildLockRequest(new LockOptions(LockMode.OPTIMISTIC_FORCE_INCREMENT)).lock(repository);

                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        Session _session = _entityManager.unwrap(Session.class);
                        Repository _repository = (Repository) _entityManager.find(Repository.class, 1L);
                        _session.buildLockRequest(new LockOptions(LockMode.OPTIMISTIC_FORCE_INCREMENT)).lock(_repository);
                        Commit _commit = new Commit(_repository);
                        _commit.getChanges().add(new Change("index.html", "0a1,2..."));
                        _entityManager.persist(_commit);
                    });
                });

                Commit commit = new Commit(repository);
                commit.getChanges().add(new Change("README.txt", "0a1,5..."));
                commit.getChanges().add(new Change("web.xml", "17c17..."));
                entityManager.persist(commit);
            });
            fail("Should have thrown StaleObjectStateException!");
        } catch (Exception expected) {
            assertEquals(OptimisticLockException.class, expected.getCause().getClass());
            LOGGER.info("Failure: ", expected);
        }
    }


    /**
     * Repository - Repository
     *
     * @author Carol Mihalcea
     */
    @Entity(name = "repository")
    @Table(name = "repository")
    public static class Repository {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;

        public Repository() {
        }

        public Repository(String name) {
            this.name = name;
        }

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    /**
     * Commit - Commit
     *
     * @author Carol Mihalcea
     */
    @Entity(name = "Commit")
    @Table(name = "commit")
    @Immutable
    public static class Commit {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ManyToOne
        private Repository repository;

        @ElementCollection
        @CollectionTable(
                name="commit_change",
                joinColumns=@JoinColumn(name="commit_id")
        )
        private List<Change> changes = new ArrayList<>();

        public Commit() {
        }

        public Commit(Repository repository) {
            this.repository = repository;
        }

        public Repository getRepository() {
            return repository;
        }

        public List<Change> getChanges() {
            return changes;
        }
    }

    /**
     * OrderLine - Order Line
     *
     * @author Carol Mihalcea
     */
    @Embeddable
    public static class Change {

        private String path;

        private String diff;

        public Change() {
        }

        public Change(String path, String diff) {
            this.path = path;
            this.diff = diff;
        }

        public String getPath() {
            return path;
        }

        public String getDiff() {
            return diff;
        }
    }
}
