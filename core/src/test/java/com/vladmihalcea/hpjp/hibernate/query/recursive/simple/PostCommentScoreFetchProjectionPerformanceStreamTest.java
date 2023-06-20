package com.vladmihalcea.hpjp.hibernate.query.recursive.simple;

import com.vladmihalcea.hpjp.hibernate.query.recursive.PostCommentScore;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentScoreFetchProjectionPerformanceStreamTest extends AbstractPostCommentScorePerformanceTest {

    protected com.codahale.metrics.Timer inMemoryProcessingTimer = metricRegistry.timer("In-memory processing timer");

    public PostCommentScoreFetchProjectionPerformanceStreamTest(int postCount, int commentCount) {
        super(postCount, commentCount);
    }

    @Override
    protected List<PostCommentScore> postCommentScores(Long postId, int rank) {
        long startNanos = System.nanoTime();
        AtomicLong startInMemoryProcessingNanos = new AtomicLong();
        List<PostCommentScore> roots = doInJPA(entityManager -> {
            List<PostCommentScore> postCommentScores = entityManager.createQuery("""
                select new
                    com.vladmihalcea.hpjp.hibernate.query.recursive.PostCommentScore(   
                        pc.id, pc.parent.id, pc.review, pc.createdOn, pc.score 
                    )
                from PostComment pc
                where pc.post.id = :postId
                """)
            .setParameter("postId", postId)
            .getResultList();

            startInMemoryProcessingNanos.set(System.nanoTime());

            Map<Long, PostCommentScore> postCommentScoreMap = postCommentScores
                .stream()
                .collect(Collectors.toMap(PostCommentScore::getId, Function.identity()));

            List<PostCommentScore> postCommentRoots = postCommentScores
                .stream()
                .filter(pcs -> {
                    boolean isRoot = pcs.getParentId() == null;
                    if(!isRoot) {
                        postCommentScoreMap.get(pcs.getParentId()).addChild(pcs);
                    }
                    return isRoot;
                })
                .sorted(
                    Comparator.comparing(PostCommentScore::getTotalScore).reversed()
                )
                .limit(rank)
                .collect(Collectors.toList());

            return postCommentRoots;
        });
        inMemoryProcessingTimer.update(System.nanoTime() - startInMemoryProcessingNanos.get(), TimeUnit.NANOSECONDS);
        timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        return roots;
    }
}
