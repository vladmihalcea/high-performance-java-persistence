package com.vladmihalcea.hpjp.spring.data.query.specification.repository;

import com.vladmihalcea.hpjp.spring.data.query.specification.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.specification.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.specification.domain.PostComment_;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository
    extends BaseJpaRepository<PostComment, Long>,
            JpaSpecificationExecutor<PostComment> {

    interface Specs {

        static Specification<PostComment> byPost(Post post) {
            return (root, query, builder) ->
                builder.equal(root.get(PostComment_.post), post);
        }

        static Specification<PostComment> byStatus(PostComment.Status status) {
            return (root, query, builder) ->
                builder.equal(root.get(PostComment_.status), status);
        }

        static Specification<PostComment> byReviewLike(String reviewPattern) {
            return (root, query, builder) ->
                builder.like(root.get(PostComment_.review), reviewPattern);
        }

        static Specification<PostComment> byVotesGreaterThanEqual(int votes) {
            return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get(PostComment_.votes), votes);
        }

        static Specification<PostComment> orderByCreatedOn(Specification<PostComment> spec) {
            return (root, query, builder) -> {
                query.orderBy(builder.asc(root.get(PostComment_.createdOn)));
                return spec.toPredicate(root, query, builder);
            };
        }
    }
}
