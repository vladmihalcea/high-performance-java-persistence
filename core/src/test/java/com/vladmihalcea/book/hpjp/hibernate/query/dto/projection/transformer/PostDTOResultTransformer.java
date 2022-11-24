package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer;

import org.hibernate.transform.ResultTransformer;

import java.util.List;

/**
 *
 * @author Vlad Mihalcea
 */
public class PostDTOResultTransformer extends PostDTOTupleTransformer
    implements ResultTransformer {

    @Override
    public List<PostDTO> transformList(List collection) {
        return DistinctListTransformer.INSTANCE.transformList(collection);
    }
}
