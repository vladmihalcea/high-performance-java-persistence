package com.vladmihalcea.hpjp.spring.data.query.method.repository;

import com.vladmihalcea.hpjp.spring.data.query.method.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.method.domain.PostComment;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository
    extends BaseJpaRepository<PostComment, Long>, CustomPostCommentRepository {

    List<PostComment> findAllByPost(Post post);

    List<PostComment> findAllByPostOrderByCreatedOn(Post post);

    List<PostComment> findAllByPostAndStatusOrderByCreatedOn(
        Post post,
        PostComment.Status status
    );

    List<PostComment> findAllByPostAndStatusAndReviewLikeOrderByCreatedOn(
        Post post,
        PostComment.Status status,
        String reviewPattern
    );

    List<PostComment> findAllByPostAndStatusAndReviewLikeAndVotesGreaterThanEqualOrderByCreatedOn(
        Post post,
        PostComment.Status status,
        String reviewPattern,
        int votes
    );

    @Query("""
        select pc
        from PostComment pc
        where
            pc.post = :post and
            pc.status = :status and
            pc.review like :reviewPattern and
            pc.votes >= :votes
        order by createdOn
        """)
    List<PostComment> findAllByPostStatusReviewAndMinVotes(
        @Param("post") Post post,
        @Param("status") PostComment.Status status,
        @Param("reviewPattern") String reviewPattern,
        @Param("votes") int votes
    );
}
