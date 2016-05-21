package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.Immutable;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;


/**
 * LockModePessimisticForceIncrementTest - Test to check LockMode.PESSIMISTIC_FORCE_INCREMENT
 *
 * @author Carol Mihalcea
 */
public class LockModePessimisticForceIncrementTest extends AbstractTest {

    private final CountDownLatch startLatch = new CountDownLatch(1);
    private final CountDownLatch endLatch = new CountDownLatch(1);

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
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
    public void testPessimisticForceIncrementLocking() throws InterruptedException {
        LOGGER.info("Test Single PESSIMISTIC_FORCE_INCREMENT Lock Mode ");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            Repository repository = (Repository) entityManager.find(Repository.class, 1L);
            session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_FORCE_INCREMENT)).lock(repository);
            Commit commit = new Commit(repository);
            commit.getChanges().add(new Change("README.txt", "0a1,5..."));
            commit.getChanges().add(new Change("web.xml", "17c17..."));
            entityManager.persist(commit);
        });
    }

    @Test
    public void testConcurrentPessimisticForceIncrementLockingWithLockWaiting() throws InterruptedException {
        LOGGER.info("Test Concurrent PESSIMISTIC_FORCE_INCREMENT Lock Mode With Lock Waiting");
        doInJPA(entityManager -> {
            try {
                Session session = entityManager.unwrap(Session.class);
                Repository repository = (Repository) entityManager.find(Repository.class, 1L);
                session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_FORCE_INCREMENT)).lock(repository);

                executeAsync(() -> doInJPA(_entityManager -> {
                    LOGGER.info("Try to get the Repository row");
                    startLatch.countDown();
                    Session _session = _entityManager.unwrap(Session.class);
                    Repository _repository = (Repository) _entityManager.find(Repository.class, 1L);
                    _session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_FORCE_INCREMENT)).lock(_repository);
                    Commit _commit = new Commit(_repository);
                    _commit.getChanges().add(new Change("index.html", "0a1,2..."));
                    _entityManager.persist(_commit);
                    _entityManager.flush();
                    endLatch.countDown();
                }));
                startLatch.await();
                LOGGER.info("Sleep for 500ms to delay the other transaction PESSIMISTIC_FORCE_INCREMENT Lock Mode acquisition");
                Thread.sleep(500);
                Commit commit = new Commit(repository);
                commit.getChanges().add(new Change("README.txt", "0a1,5..."));
                commit.getChanges().add(new Change("web.xml", "17c17..."));
                entityManager.persist(commit);
            } catch (InterruptedException e) {
                fail("Unexpected failure");
            }
        });
        endLatch.await();
    }

    @Test
    public void testConcurrentPessimisticForceIncrementLockingFailFast() throws InterruptedException {
        LOGGER.info("Test Concurrent PESSIMISTIC_FORCE_INCREMENT Lock Mode fail fast");
        doInJPA(entityManager -> {
            try {
                Session session = entityManager.unwrap(Session.class);
                Repository repository = (Repository) entityManager.find(Repository.class, 1L);

                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        Session _session = _entityManager.unwrap(Session.class);
                        Repository _repository = (Repository) _entityManager.find(Repository.class, 1L);
                        _session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_FORCE_INCREMENT)).lock(_repository);
                        Commit _commit = new Commit(_repository);
                        _commit.getChanges().add(new Change("index.html", "0a1,2..."));
                        _entityManager.persist(_commit);
                        _entityManager.flush();
                    });
                });
                session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_FORCE_INCREMENT)).lock(repository);
                fail("Should have thrown StaleObjectStateException!");
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Failure: ", expected);
            }
        });
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
     * OrderLine - Order Line
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
                name = "commit_change",
                joinColumns = @JoinColumn(name = "commit_id")
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
