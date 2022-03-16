package com.vladmihalcea.book.hpjp.hibernate.fetching.detector;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vlad Mihalcea
 */
public class AssociationFetchPreLoadEventListener implements LoadEventListener {

    public static final AssociationFetchPreLoadEventListener INSTANCE = new AssociationFetchPreLoadEventListener();

    @Override
    public void onLoad(LoadEvent event, LoadType loadType) {
        AssociationFetch.Context
            .get(event.getSession())
            .preLoad(event);
    }
}
