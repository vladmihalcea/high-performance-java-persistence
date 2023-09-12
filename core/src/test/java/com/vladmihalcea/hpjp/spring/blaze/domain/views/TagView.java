package com.vladmihalcea.hpjp.spring.blaze.domain.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.vladmihalcea.hpjp.spring.blaze.domain.Tag;

/**
 * @author Vlad Mihalcea
 */
@EntityView(Tag.class)
public interface TagView {
    @IdMapping
    Long getId();

    String getName();
}
