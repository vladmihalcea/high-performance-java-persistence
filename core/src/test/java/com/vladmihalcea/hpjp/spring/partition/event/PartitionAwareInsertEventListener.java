package com.vladmihalcea.hpjp.spring.partition.event;

import com.vladmihalcea.hpjp.spring.partition.domain.PartitionAware;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.internal.FilterImpl;

/**
 * @author Vlad Mihalcea
 */
public class PartitionAwareInsertEventListener implements PersistEventListener {

    public static final PartitionAwareInsertEventListener INSTANCE = new PartitionAwareInsertEventListener();

    @Override
    public void onPersist(PersistEvent event) throws HibernateException {
        final Object entity = event.getObject();

        if (entity instanceof PartitionAware partitionAware) {
            if (partitionAware.getPartitionKey() == null) {
                FilterImpl partitionKeyFilter = (FilterImpl) event
                    .getSession()
                    .getEnabledFilter(PartitionAware.PARTITION_KEY);
                partitionAware.setPartitionKey(
                    (String) partitionKeyFilter
                        .getParameter(PartitionAware.PARTITION_KEY)
                );
            }
        }
    }

    @Override
    public void onPersist(PersistEvent event, PersistContext persistContext)
            throws HibernateException {
        onPersist(event);
    }
}
