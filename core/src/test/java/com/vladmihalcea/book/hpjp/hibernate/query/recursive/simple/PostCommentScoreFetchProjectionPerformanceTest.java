package com.vladmihalcea.book.hpjp.hibernate.query.recursive.simple;

import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentScoreFetchProjectionPerformanceTest extends AbstractPostCommentScorePerformanceTest {

    protected com.codahale.metrics.Timer inMemoryProcessingTimer = metricRegistry.timer("In-memory processing timer");

    public PostCommentScoreFetchProjectionPerformanceTest(int postCount, int commentCount) {
        super(postCount, commentCount);
    }

    @Override
    protected List<PostCommentScore> postCommentScores(Long postId, int rank) {
        return doInJPA(entityManager -> {
            long startNanos = System.nanoTime();
            List<PostCommentScore> postCommentScores = entityManager.createQuery(
                "select new " +
                "   com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore(" +
                "   pc.id, pc.parent.id, pc.review, pc.createdOn, pc.score ) " +
                "from PostComment pc " +
                "where pc.post.id = :postId ")
            .setParameter("postId", postId)
            .getResultList();

            long startInMemoryProcessingNanos = System.nanoTime();
            List<PostCommentScore> roots = new ArrayList<>();

            if (!postCommentScores.isEmpty()) {
                Map<Long, PostCommentScore> postCommentScoreMap = new HashMap<>();
                for(PostCommentScore postCommentScore : postCommentScores) {
                    Long id = postCommentScore.getId();
                    if (!postCommentScoreMap.containsKey(id)) {
                        postCommentScoreMap.put(id, postCommentScore);
                    }
                }

                for(PostCommentScore postCommentScore : postCommentScores) {
                    Long parentId = postCommentScore.getParentId();
                    if(parentId == null) {
                        roots.add(postCommentScore);
                    } else {
                        PostCommentScore parent = postCommentScoreMap.get(parentId);
                        parent.addChild(postCommentScore);
                    }
                }

                roots.sort(
                    Comparator.comparing(PostCommentScore::getTotalScore).reversed()
                );

                if(roots.size() > rank) {
                    roots = roots.subList(0, rank);
                }
            }
            inMemoryProcessingTimer.update(System.nanoTime() - startInMemoryProcessingNanos, TimeUnit.NANOSECONDS);
            timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            return  roots;
        });
    }
}
