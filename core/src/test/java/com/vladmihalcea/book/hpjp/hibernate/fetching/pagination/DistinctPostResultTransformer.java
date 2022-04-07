package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import jakarta.persistence.EntityManager;
import org.hibernate.query.ResultListTransformer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class DistinctPostResultTransformer implements ResultListTransformer {

    private final EntityManager entityManager;

    public DistinctPostResultTransformer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List transformList(List list) {
        Map<Serializable, Identifiable> identifiableMap = new LinkedHashMap<>(list.size());
        for (Object entityArray : list) {
            if (Object[].class.isAssignableFrom(entityArray.getClass())) {
                Post post = null;
                PostComment comment = null;

                Object[] tuples = (Object[]) entityArray;

                for (Object tuple : tuples) {
                    if(tuple instanceof Identifiable) {
                        entityManager.detach(tuple);

                        if (tuple instanceof Post) {
                            post = (Post) tuple;
                        } else if (tuple instanceof PostComment) {
                            comment = (PostComment) tuple;
                        } else {
                            throw new UnsupportedOperationException(
                                    "Tuple " + tuple.getClass() + " is not supported!"
                            );
                        }
                    }
                }

                if (post != null) {
                    if (!identifiableMap.containsKey(post.getId())) {
                        identifiableMap.put(post.getId(), post);
                        post.setComments(new ArrayList<>());
                    }
                    if (comment != null) {
                        post.addComment(comment);
                    }
                }
            }
        }
        return new ArrayList<>(identifiableMap.values());
    }
}
