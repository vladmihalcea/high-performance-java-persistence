package com.vladmihalcea.hpjp.spring.blaze.domain.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.vladmihalcea.hpjp.spring.blaze.domain.UserVote;

/**
 * @author Vlad Mihalcea
 */
@EntityView(UserVote.class)
public interface UserVoteView {
    @IdMapping
    Long getId();

    UserView getUser();

    int getScore();
}
