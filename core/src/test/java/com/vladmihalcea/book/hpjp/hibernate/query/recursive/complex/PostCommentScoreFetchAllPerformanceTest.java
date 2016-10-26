package com.vladmihalcea.book.hpjp.hibernate.query.recursive.complex;

import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentScoreFetchAllPerformanceTest extends AbstractPostCommentScorePerformanceTest {

    public PostCommentScoreFetchAllPerformanceTest(int postCount, int commentCount) {
        super(postCount, commentCount);
    }

    @Override
    protected List<PostCommentScore> postCommentScores(Long postId, int rank) {
        return doInJPA(entityManager -> {
            long startNanos = System.nanoTime();
            List<PostCommentVote> postCommentVotes = entityManager.createQuery(
                "select pcv " +
                "from PostCommentVote pcv " +
                "left join fetch pcv.comment pc " +
                "left join fetch pc.parent pcp " +
                "where pc.post.id = :postId", PostCommentVote.class)
            .setParameter("postId", postId)
            .getResultList();

            Map<Long, PostCommentScore> postCommentScoreMap = new HashMap<>();

            for(PostCommentVote postCommentVote : postCommentVotes) {
                PostComment postComment = postCommentVote.getComment();
                PostComment parent = postComment.getParent();
                PostCommentScore postCommentScore = postCommentScoreMap.get(postComment.getId());
                if(postCommentScore == null) {
                    postCommentScore = new PostCommentScore();
                    postCommentScore.setId(postComment.getId());
                    postCommentScore.setReview(postComment.getReview());
                    postCommentScore.setCreatedOn(postComment.getCreatedOn());
                    postCommentScore.setParentId(parent != null ? parent.getId() : null);
                    postCommentScore.setScore(postCommentScore.getScore() + (postCommentVote.isUp() ? 1 : -1));
                    postCommentScoreMap.put(postComment.getId(), postCommentScore);
                }
            }

            List<PostCommentScore> roots = new ArrayList<>();

            for(PostCommentScore postCommentScore : postCommentScoreMap.values()) {
                Long parentId = postCommentScore.getParentId();
                if(parentId == null) {
                    roots.add(postCommentScore);
                } else {
                    PostCommentScore parent = postCommentScoreMap.get(parentId);
                    parent.addChild(postCommentScore);
                }
            }

            roots.sort(Comparator.comparing(PostCommentScore::getTotalScore).reversed());

            if(roots.size() > rank) {
                roots = roots.subList(0, rank);
            }
            timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            return  roots;
        });
    }
}
