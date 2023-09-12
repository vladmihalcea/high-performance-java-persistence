package com.vladmihalcea.hpjp.spring.blaze.domain.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.vladmihalcea.hpjp.spring.blaze.domain.Post;

/**
 * @author Vlad Mihalcea
 */
@EntityView(Post.class)
public interface PostView {
    @IdMapping
    Long getId();

    String getTitle();
}
