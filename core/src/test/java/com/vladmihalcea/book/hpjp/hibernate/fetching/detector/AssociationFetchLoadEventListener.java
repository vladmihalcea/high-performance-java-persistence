package com.vladmihalcea.book.hpjp.hibernate.fetching.detector;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vlad Mihalcea
 */
public class AssociationFetchLoadEventListener implements LoadEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssociationFetchLoadEventListener.class);

    public static final AssociationFetchLoadEventListener INSTANCE = new AssociationFetchLoadEventListener();

    @Override
    public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
        AssociationFetch.Context
            .get(event.getSession())
            .load(event);
    }
}
