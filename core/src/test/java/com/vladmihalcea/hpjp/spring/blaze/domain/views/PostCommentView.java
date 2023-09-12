package com.vladmihalcea.hpjp.spring.blaze.domain.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import com.vladmihalcea.hpjp.spring.blaze.domain.PostComment;

import java.util.List;

import static com.blazebit.persistence.view.FetchStrategy.MULTISET;

/**
 * @author Vlad Mihalcea
 */
@EntityView(PostComment.class)
public interface PostCommentView {
    @IdMapping
    Long getId();

    String getReview();

    @Mapping(fetch = MULTISET)
    List<UserVoteView> getVotes();
}
