package com.vladmihalcea.hpjp.spring.transaction.contract.event;

import com.vladmihalcea.hpjp.spring.transaction.contract.domain.RootAware;
import jakarta.persistence.LockModeType;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vlad Mihalcea
 */
public class RootAwareInsertEventListener implements PersistEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootAwareInsertEventListener.class);

    public static final RootAwareInsertEventListener INSTANCE = new RootAwareInsertEventListener();

    @Override
    public void onPersist(PersistEvent event) throws HibernateException {
        final Object entity = event.getObject();

        if (entity instanceof RootAware rootAware) {
            Object root = rootAware.root();
            event.getSession().lock(root, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

            LOGGER.info(
                "Incrementing the [{}] entity version " +
                "because the [{}] child entity has been inserted",
                root,
                entity
            );
        }
    }

    @Override
    public void onPersist(PersistEvent event, PersistContext persistContext)
            throws HibernateException {
        onPersist(event);
    }
}
