package com.vladmihalcea.hpjp.hibernate.fetching.detector;

import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

/**
 * @author Vlad Mihalcea
 */
public class AssociationFetchLoadEventListener implements LoadEventListener {

    public static final AssociationFetchLoadEventListener INSTANCE = new AssociationFetchLoadEventListener();

    @Override
    public void onLoad(LoadEvent event, LoadType loadType) {
        AssociationFetch.Context
            .get(event.getSession())
            .load(event);
    }
}
