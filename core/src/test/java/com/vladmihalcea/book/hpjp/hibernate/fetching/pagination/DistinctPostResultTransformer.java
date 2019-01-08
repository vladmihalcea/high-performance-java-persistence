package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import org.hibernate.transform.BasicTransformerAdapter;

import java.io.Serializable;
import java.util.*;

/**
 * @author Vlad Mihalcea
 */
public class DistinctPostResultTransformer extends BasicTransformerAdapter {

    public static final DistinctPostResultTransformer INSTANCE = new DistinctPostResultTransformer();

    @Override
    public List transformList(List list) {
        Map<Serializable, Identifiable> identifiableMap = new LinkedHashMap<>(list.size());
        for (Object entityArray : list) {
            if (Object[].class.isAssignableFrom(entityArray.getClass())) {
                Post post = null;
                PostComment comment = null;

                Object[] tuples = (Object[]) entityArray;

                for (Object tuple : tuples) {
                    if (tuple instanceof Post) {
                        post = (Post) tuple;
                    } else if (tuple instanceof PostComment) {
                        comment = (PostComment) tuple;
                    } else if (tuple != null) {

                        // because it is possible to exist post without comments

                        throw new UnsupportedOperationException(
                                "Tuple " + tuple.getClass() + " is not supported!"
                        );
                    }
                }
                Objects.requireNonNull(post);

                if (!identifiableMap.containsKey(post.getId())) {
                    identifiableMap.put(post.getId(), post);
                    post.setComments(new ArrayList<>());
                }

                if (comment != null) {
                    post.addComment(comment);
                }

            }
        }
        return new ArrayList<>(identifiableMap.values());
    }
}
