package com.vladmihalcea.book.hpjp.hibernate.query.recursive.simple;

import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <code>PostCommentScoreTest</code> - PostCommentScore Test
 *
 * @author Vlad Mihalcea
 */
public class PostCommentScoreFetchProjectionPerformanceTest extends AbstractPostCommentScorePerformanceTest {

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

            Map<Long, List<PostCommentScore>> postCommentScoreMap = postCommentScores.stream().collect(Collectors.groupingBy(PostCommentScore::getId));

            List<PostCommentScore> roots = new ArrayList<>();

            for(PostCommentScore postCommentScore : postCommentScores) {
                Long parentId = postCommentScore.getParentId();
                if(parentId == null) {
                    roots.add(postCommentScore);
                } else {
                    PostCommentScore parent = postCommentScoreMap.get(parentId).get(0);
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
