package com.vladmihalcea.hpjp.hibernate.fetching.detector;

import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

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
