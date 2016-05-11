package com.vladmihalcea.book.hpjp.hibernate.cache.readwrite;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;


/**
 * NonStrictReadWriteCacheConcurrencyStrategyTest - Test to check CacheConcurrencyStrategy.READ_WRITE
 *
 * @author Vlad Mihalcea
 */
public class RepositoryReadWriteCacheConcurrencyStrategyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Repository.class,
                Commit.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        return properties;
    }

    private Repository repositoryReference;

    @Before
    public void init() {
        super.init();
        repositoryReference = doInJPA(entityManager -> {
            LOGGER.info("Read-write entities are write-through on persisting");
            Repository repository = new Repository("Hibernate-Master-Class");
            Commit commit = new Commit(repository);
            commit.getChanges().add(
                    new Change("README.txt", "0a1,5...")
            );
            commit.getChanges().add(
                    new Change("web.xml", "17c17...")
            );
            repository.addCommit(commit);
            entityManager.persist(repository);
            return repository;
        });
    }

    @Test
    public void testRepositoryEntityUpdate() {
        LOGGER.info("Read-write entities are write-through on updating");
        doInJPA(entityManager -> {
            Repository repository = (Repository) entityManager.find(Repository.class, repositoryReference.getId());
            repository.setName("High-Performance Hibernate");
            for(Commit commit : repository.commits) {
                for(Change change : commit.changes) {
                    assertNotNull(change.getDiff());
                }
            }
        });
        doInJPA(entityManager -> {
            LOGGER.info("Reload entity after updating");
            Repository repository = (Repository) entityManager.find(Repository.class, repositoryReference.getId());
            assertEquals("High-Performance Hibernate", repository.getName());
        });
    }

    @Test
    public void testRepositoryEntityDelete() {
        LOGGER.info("Read-write entities are deletable");
        doInJPA(entityManager -> {
            Repository repository = (Repository) entityManager.find(Repository.class, repositoryReference.getId());
            entityManager.remove(repository);
        });
        doInJPA(entityManager -> {
            assertNull(entityManager.find(Repository.class, repositoryReference.getId()));
        });
    }

    @Test
    public void testOptimisticLocking() {
        LOGGER.info("testOptimisticLocking");
        doInJPA(entityManager -> {
            LOGGER.info("Load Repository");
            Repository repository = entityManager.find(Repository.class, 1L);
            entityManager.unwrap(Session.class).buildLockRequest(new LockOptions().setLockMode(LockMode.OPTIMISTIC)).lock(repository);
        });
        doInJPA(entityManager -> {
            LOGGER.info("Load Repository again");
            Repository repository = entityManager.find(Repository.class, 1L);
            LOGGER.info("After load Repository again");
        });
    }

    /**
     * Repository - Repository
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "repository")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Repository {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;

        @Version
        private int version;

        @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL)
        @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
        private List<Commit> commits = new ArrayList<>();

        public Repository() {
        }

        public Repository(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void addCommit(Commit commit) {
            commits.add(commit);
            commit.repository = this;
        }
    }

    /**
     * Commit - Commit
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "Commit")
    @Table(name = "commit")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @Immutable
    public static class Commit {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Repository repository;

        @ElementCollection
        @CollectionTable(
                name="commit_change",
                joinColumns=@JoinColumn(name="commit_id")
        )
        @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
     * Change - Change
     *
     * @author Vlad Mihalcea
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
