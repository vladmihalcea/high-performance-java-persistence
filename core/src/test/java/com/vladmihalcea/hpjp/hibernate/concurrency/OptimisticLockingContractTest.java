package com.vladmihalcea.hpjp.hibernate.concurrency;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vlad Mihalcea
 */
public class OptimisticLockingContractTest extends AbstractTest {

    public static class RootAwareEventListenerIntegrator implements Integrator {

        public static final RootAwareEventListenerIntegrator INSTANCE = new RootAwareEventListenerIntegrator();

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            final EventListenerRegistry eventListenerRegistry =
                    serviceRegistry.getService( EventListenerRegistry.class );

            eventListenerRegistry.appendListeners(EventType.PERSIST, RootAwareInsertEventListener.INSTANCE);
            eventListenerRegistry.appendListeners(EventType.FLUSH_ENTITY, RootAwareUpdateAndDeleteEventListener.INSTANCE);
        }

        @Override
        public void disintegrate(
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

        }
    }

    public static class RootAwareInsertEventListener implements PersistEventListener {

        private static final Logger LOGGER = LoggerFactory.getLogger(RootAwareInsertEventListener.class);

        public static final RootAwareInsertEventListener INSTANCE = new RootAwareInsertEventListener();

        @Override
        public void onPersist(PersistEvent event) throws HibernateException {
            final Object entity = event.getObject();

            if(entity instanceof RootAware) {
                RootAware rootAware = (RootAware) entity;
                Object root = rootAware.root();
                event.getSession().lock(root, LockMode.OPTIMISTIC_FORCE_INCREMENT);

                LOGGER.info("Incrementing {} entity version because a {} child entity has been inserted", root, entity);
            }
        }

        @Override
        public void onPersist(PersistEvent event, PersistContext persistContext) throws HibernateException {
            onPersist(event);
        }
    }

    public static class RootAwareUpdateAndDeleteEventListener implements FlushEntityEventListener {

        private static final Logger LOGGER = LoggerFactory.getLogger(RootAwareUpdateAndDeleteEventListener.class);

        public static final RootAwareUpdateAndDeleteEventListener INSTANCE = new RootAwareUpdateAndDeleteEventListener();

        @Override
        public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
            final EntityEntry entry = event.getEntityEntry();
            final Object entity = event.getEntity();
            final boolean mightBeDirty = entry.requiresDirtyCheck( entity );

            if(mightBeDirty && entity instanceof RootAware) {
                RootAware rootAware = (RootAware) entity;
                if(updated(event)) {
                    Object root = rootAware.root();
                    LOGGER.info("Incrementing {} entity version because a {} child entity has been updated", root, entity);
                    incrementRootVersion(event, root);
                }
                else if (deleted(event)) {
                    Object root = rootAware.root();
                    LOGGER.info("Incrementing {} entity version because a {} child entity has been deleted", root, entity);
                    incrementRootVersion(event, root);
                }
            }
        }

        private void incrementRootVersion(FlushEntityEvent event, Object root) {
            event.getSession().lock(root, LockMode.OPTIMISTIC_FORCE_INCREMENT);
        }

        private boolean deleted(FlushEntityEvent event) {
            return event.getEntityEntry().getStatus() == Status.DELETED;
        }

        private boolean updated(FlushEntityEvent event) {
            final EntityEntry entry = event.getEntityEntry();
            final Object entity = event.getEntity();

            int[] dirtyProperties;
            EntityPersister persister = entry.getPersister();
            final Object[] values = event.getPropertyValues();
            SessionImplementor session = event.getSession();

            if ( event.hasDatabaseSnapshot() ) {
                dirtyProperties = persister.findModified( event.getDatabaseSnapshot(), values, entity, session );
            }
            else {
                dirtyProperties = persister.findDirty( values, entry.getLoadedState(), entity, session );
            }

            return dirtyProperties != null;
        }
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Contract.class,
            Annex.class,
            Signature.class,
        };
    }

    @Override
    protected Integrator integrator() {
        return RootAwareEventListenerIntegrator.INSTANCE;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Contract contract = new Contract()
                .setId(1L)
                .setTitle("Hypersistence Training");

            Annex annex1 = new Annex()
                .setId(1L)
                .setDetails("High-Performance Java Persistence Training")
                .setContract(contract);

            Signature signature1 = new Signature()
                .setAnnex(annex1)
                .setUserName("Vlad Mihalcea");

            Annex annex2 = new Annex()
                .setId(2L)
                .setDetails("High-Performance SQL Training")
                .setContract(contract);

            Signature signature2 = new Signature()
                .setAnnex(annex2)
                .setUserName("Vlad Mihalcea");

            entityManager.persist(contract);
            entityManager.persist(annex1);
            entityManager.persist(annex2);
            entityManager.persist(signature1);
            entityManager.persist(signature2);
        });

        doInJPA(entityManager -> {
            Annex annex = entityManager.createQuery("""
                select a
                from Annex a
                join fetch a.contract c
                where a.id = :id
                """, Annex.class)
            .setParameter("id", 2L)
            .getSingleResult();

            annex.setDetails("High-Performance SQL Online Training");
        });

        doInJPA(entityManager -> {
            Contract contract = entityManager.getReference(Contract.class, 1L);

            Annex annex = new Annex()
                .setId(3L)
                .setDetails("Spring 6 Migration Training")
                .setContract(contract);

            entityManager.persist(annex);
        });

        doInJPA(entityManager -> {
            Annex annex = entityManager.getReference(Annex.class, 3L);
            entityManager.remove(annex);
        });
    }

    public interface RootAware<T> {
        T root();
    }

    @Entity(name = "Contract")
    @Table(name = "contract")
    public static class Contract {

        @Id
        private Long id;

        private String title;

        @Version
        private Short version;

        public Long getId() {
            return id;
        }

        public Contract setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Contract setTitle(String title) {
            this.title = title;
            return this;
        }
    }

    @Entity(name = "Annex")
    @Table(name = "annex")
    public static class Annex implements RootAware<Contract> {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Contract contract;

        private String details;

        public Long getId() {
            return id;
        }

        public Annex setId(Long id) {
            this.id = id;
            return this;
        }

        public Contract getContract() {
            return contract;
        }

        public Annex setContract(Contract post) {
            this.contract = post;
            return this;
        }

        public String getDetails() {
            return details;
        }

        public Annex setDetails(String review) {
            this.details = review;
            return this;
        }

        @Override
        public Contract root() {
            return contract;
        }
    }

    @Entity(name = "Signature")
    @Table(name = "signature")
    public static class Signature implements RootAware<Contract> {

        @Id
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        @OnDelete( action = OnDeleteAction.CASCADE )
        private Annex annex;

        private String userName;

        public Long getId() {
            return id;
        }

        public Signature setId(Long id) {
            this.id = id;
            return this;
        }

        public Annex getAnnex() {
            return annex;
        }

        public Signature setAnnex(Annex comment) {
            this.annex = comment;
            return this;
        }

        public String getUserName() {
            return userName;
        }

        public Signature setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        @Override
        public Contract root() {
            return annex.root();
        }
    }
}
