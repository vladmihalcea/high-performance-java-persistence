package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.Immutable;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * LockModePessimisticForceIncrementTest - Test to check LockMode.PESSIMISTIC_FORCE_INCREMENT
 *
 * @author Vlad Mihalcea
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
            Repository repository = entityManager.find(Repository.class, 1L,
                LockModeType.PESSIMISTIC_FORCE_INCREMENT);

            Commit commit = new Commit(repository);
            commit.getChanges().add(new Change("FrontMatter.md", "0a1,5..."));
            commit.getChanges().add(new Change("HibernateIntro.md", "17c17..."));

            entityManager.persist(commit);
        });
    }

    @Test
    public void testConcurrentPessimisticForceIncrementLockingWithLockWaiting() throws InterruptedException {
        LOGGER.info("Test Concurrent PESSIMISTIC_FORCE_INCREMENT Lock Mode With Lock Waiting");
        doInJPA(entityManager -> {
            Repository repository = entityManager.find(Repository.class, 1L, LockModeType.PESSIMISTIC_FORCE_INCREMENT);

            executeAsync(() -> doInJPA(_entityManager -> {
                startLatch.countDown();
                Repository _repository = _entityManager.find(Repository.class, 1L, LockModeType.PESSIMISTIC_FORCE_INCREMENT);

                Commit _commit = new Commit(_repository);
                _commit.getChanges().add(new Change("Intro.md", "0a1,2..."));
                _entityManager.persist(_commit);
                _entityManager.flush();
                endLatch.countDown();
            }));
            awaitOnLatch(startLatch);
            LOGGER.info("Sleep for 500ms to delay the other transaction");
            sleep(500);

            Commit commit = new Commit(repository);
            commit.getChanges().add(new Change("FrontMatter.md", "0a1,5..."));
            commit.getChanges().add(new Change("HibernateIntro.md", "17c17..."));

            entityManager.persist(commit);
        });
        endLatch.await();
    }

    @Test
    public void testConcurrentPessimisticForceIncrementLockingFailFast() {
        try {
            doInJPA(entityManager -> {
                Repository repository = entityManager.find(Repository.class, 1L);

                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        Repository _repository = _entityManager.find(Repository.class, 1L,
                            LockModeType.PESSIMISTIC_FORCE_INCREMENT);

                        Commit _commit = new Commit(_repository);
                        _commit.getChanges().add(new Change("Intro.md", "0a1,2..."));

                        _entityManager.persist(_commit);
                    });
                });

                entityManager.lock(repository, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                fail("Should have thrown StaleObjectStateException!");
            });
        } catch (OptimisticLockException expected) {
            assertEquals(StaleObjectStateException.class, expected.getCause().getClass());
            LOGGER.info("Failure: ", expected);
        }
    }

    @Entity(name = "Repository")
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

        public Commit() {}

        public Commit(Repository repository) {
            this.repository = repository;
        }

        public Repository getRepository() {
            return repository;
        }

        public void setRepository(Repository repository) {
            this.repository = repository;
        }

        public List<Change> getChanges() {
            return changes;
        }
    }

    @Embeddable
    public static class Change {

        private String path;

        private String diff;

        public Change() {}

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
