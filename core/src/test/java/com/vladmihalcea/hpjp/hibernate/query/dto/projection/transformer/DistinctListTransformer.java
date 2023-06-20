package com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer;

import org.hibernate.query.ResultListTransformer;

import java.util.List;

/**
 *
 * @author Vlad Mihalcea
 */
public class DistinctListTransformer implements ResultListTransformer {

    public static final DistinctListTransformer INSTANCE = new DistinctListTransformer();

    @Override
    public List<PostDTO> transformList(List collection) {
        return collection.stream().distinct().toList();
    }
}
