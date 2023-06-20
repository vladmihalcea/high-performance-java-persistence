package com.vladmihalcea.hpjp.hibernate.audit.hibernate.listener;

import com.vladmihalcea.hpjp.hibernate.audit.hibernate.model.Auditable;
import com.vladmihalcea.hpjp.hibernate.audit.hibernate.model.LoadEventLogEntry;
import com.vladmihalcea.hpjp.hibernate.audit.hibernate.model.LoggedUser;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Vlad Mihalcea
 */
public class AuditLogPostLoadEventListener implements PostLoadEventListener {

    public static final AuditLogPostLoadEventListener INSTANCE = new AuditLogPostLoadEventListener();

    @Override
    public void onPostLoad(PostLoadEvent event) {
        final Object entity = event.getEntity();
        final EntityPersister entityPersister = event.getPersister();

        if (entity instanceof Auditable) {
            Auditable auditable = (Auditable) entity;

            event.getSession().persist(
                new LoadEventLogEntry()
                    .setCreatedBy(LoggedUser.get())
                    .setEntityName(entityPersister.getEntityName())
                    .setEntityId(String.valueOf(auditable.getId()))
            );
        }
    }
}
