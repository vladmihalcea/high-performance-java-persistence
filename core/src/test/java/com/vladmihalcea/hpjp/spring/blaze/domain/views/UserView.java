package com.vladmihalcea.hpjp.spring.blaze.domain.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.vladmihalcea.hpjp.spring.blaze.domain.User;

/**
 * @author Vlad Mihalcea
 */
@EntityView(User.class)
public interface UserView {
    @IdMapping
    Long getId();

    String getFirstName();

    String getLastName();
}
