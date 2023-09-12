package com.vladmihalcea.hpjp.spring.blaze.domain.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.Mapping;
import com.vladmihalcea.hpjp.spring.blaze.domain.Post;

import java.util.List;

import static com.blazebit.persistence.view.FetchStrategy.MULTISET;

/**
 * @author Vlad Mihalcea
 */
@EntityView(Post.class)
public interface PostWithCommentsAndTagsView extends PostView {

    @Mapping(fetch = MULTISET)
    List<PostCommentView> getComments();

    @Mapping(fetch = MULTISET)
    List<TagView> getTags();
}
