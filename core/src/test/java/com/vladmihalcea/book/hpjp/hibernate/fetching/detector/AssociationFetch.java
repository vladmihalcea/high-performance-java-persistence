package com.vladmihalcea.book.hpjp.hibernate.fetching.detector;

import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.PostLoadEvent;

import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author Vlad Mihalcea
 */
public class AssociationFetch {

    private final Object entity;

    public AssociationFetch(Object entity) {
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }

    public static class Context implements Serializable {
        public static final String SESSION_PROPERTY_KEY = "ASSOCIATION_FETCH_LIST";

        private Map<String, Integer> entityFetchCountByClassNameMap = new LinkedHashMap<>();

        private Set<EntityIdentifier> joinedFetchedEntities = new LinkedHashSet<>();

        private Set<EntityIdentifier> secondaryFetchedEntities = new LinkedHashSet<>();

        private Map<EntityIdentifier, Object> loadedEntities = new LinkedHashMap<>();

        public List<AssociationFetch> getAssociationFetches() {
            List<AssociationFetch> associationFetches = new ArrayList<>();

            for(Map.Entry<EntityIdentifier, Object> loadedEntityMapEntry : loadedEntities.entrySet()) {
                EntityIdentifier entityIdentifier = loadedEntityMapEntry.getKey();
                Object entity = loadedEntityMapEntry.getValue();
                if(joinedFetchedEntities.contains(entityIdentifier) ||
                   secondaryFetchedEntities.contains(entityIdentifier)) {
                    associationFetches.add(new AssociationFetch(entity));
                }
            }

            return associationFetches;
        }

        public List<AssociationFetch> getJoinedAssociationFetches() {
            List<AssociationFetch> associationFetches = new ArrayList<>();

            for(Map.Entry<EntityIdentifier, Object> loadedEntityMapEntry : loadedEntities.entrySet()) {
                EntityIdentifier entityIdentifier = loadedEntityMapEntry.getKey();
                Object entity = loadedEntityMapEntry.getValue();
                if(joinedFetchedEntities.contains(entityIdentifier)) {
                    associationFetches.add(new AssociationFetch(entity));
                }
            }

            return associationFetches;
        }

        public List<AssociationFetch> getSecondaryAssociationFetches() {
            List<AssociationFetch> associationFetches = new ArrayList<>();

            for(Map.Entry<EntityIdentifier, Object> loadedEntityMapEntry : loadedEntities.entrySet()) {
                EntityIdentifier entityIdentifier = loadedEntityMapEntry.getKey();
                Object entity = loadedEntityMapEntry.getValue();
                if(secondaryFetchedEntities.contains(entityIdentifier)) {
                    associationFetches.add(new AssociationFetch(entity));
                }
            }

            return associationFetches;
        }

        public Map<Class, List<Object>> getAssociationFetchEntityMap() {
            return getAssociationFetches()
                .stream()
                .map(AssociationFetch::getEntity)
                .collect(groupingBy(Object::getClass));
        }

        public void preLoad(LoadEvent loadEvent) {
            String entityClassName = loadEvent.getEntityClassName();
            entityFetchCountByClassNameMap.put(entityClassName, SessionStatistics.getEntityFetchCount(entityClassName));
        }

        public void load(LoadEvent loadEvent) {
            String entityClassName = loadEvent.getEntityClassName();
            int previousFetchCount = entityFetchCountByClassNameMap.get(entityClassName);
            int currentFetchCount = SessionStatistics.getEntityFetchCount(entityClassName);

            EntityIdentifier entityIdentifier = new EntityIdentifier(
                ReflectionUtils.getClass(loadEvent.getEntityClassName()),
                loadEvent.getEntityId()
            );

            if (loadEvent.isAssociationFetch()) {
                if (currentFetchCount == previousFetchCount) {
                    joinedFetchedEntities.add(entityIdentifier);
                } else if (currentFetchCount > previousFetchCount){
                    secondaryFetchedEntities.add(entityIdentifier);
                }
            }
        }

        public void postLoad(PostLoadEvent postLoadEvent) {
            loadedEntities.put(
                new EntityIdentifier(
                    postLoadEvent.getEntity().getClass(),
                    postLoadEvent.getId()
                ),
                postLoadEvent.getEntity()
            );
        }

        public static Context get(Session session) {
            Context context = (Context) session.getProperties().get(SESSION_PROPERTY_KEY);
            if (context == null) {
                context = new Context();
                session.setProperty(SESSION_PROPERTY_KEY, context);
            }
            return context;
        }

        public static Context get(EntityManager entityManager) {
            return get(entityManager.unwrap(Session.class));
        }
    }

    private static class EntityIdentifier {
        private final Class entityClass;

        private final Object entityId;

        public EntityIdentifier(Class entityClass, Object entityId) {
            this.entityClass = entityClass;
            this.entityId = entityId;
        }

        public Class getEntityClass() {
            return entityClass;
        }

        public Object getEntityId() {
            return entityId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EntityIdentifier)) return false;
            EntityIdentifier that = (EntityIdentifier) o;
            return Objects.equals(getEntityClass(), that.getEntityClass()) && Objects.equals(getEntityId(), that.getEntityId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getEntityClass(), getEntityId());
        }
    }
}
