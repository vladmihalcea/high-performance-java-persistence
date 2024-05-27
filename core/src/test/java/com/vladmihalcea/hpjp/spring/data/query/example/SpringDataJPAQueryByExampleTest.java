package com.vladmihalcea.hpjp.spring.data.query.example;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.query.example.config.SpringDataJPAQueryByExampleConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.example.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.example.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.example.domain.PostComment_;
import com.vladmihalcea.hpjp.spring.data.query.example.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.query.example.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.query.example.repository.PostRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAQueryByExampleConfiguration.class)
public class SpringDataJPAQueryByExampleTest extends AbstractSpringTest {

    public static final int POST_COUNT = 2;
    public static final int POST_COMMENT_COUNT = 10;
    public static final int TAG_COUNT = 10;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class,
            Tag.class
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                List<Tag> tags = new ArrayList<>();

                for (long i = 1; i <= TAG_COUNT; i++) {
                    Tag tag = new Tag()
                        .setId(i)
                        .setName(String.format("Tag nr. %d", i));

                    entityManager.persist(tag);
                    tags.add(tag);
                }

                LocalDateTime timestamp = LocalDateTime.of(
                    2023, 3, 15, 12, 0, 0, 0
                );

                long commentId = 0;

                for (long postId = 1; postId <= POST_COUNT; postId++) {
                    Post post = new Post()
                        .setId(postId)
                        .setTitle(String.format("Post nr. %d", postId));

                    for (long i = 1; i <= POST_COMMENT_COUNT; i++) {
                        PostComment comment = new PostComment()
                            .setId(++commentId)
                            .setReview(i % 7 == 0 ? "Spam comment" : String.format("Awesome post %d", i))
                            .setStatus(PostComment.Status.PENDING)
                            .setCreatedOn(timestamp.plusMinutes(postId))
                            .setVotes((int) (i % 7));

                        post.addComment(comment);
                    }

                    for (int i = 0; i < TAG_COUNT; i++) {
                        post.getTags().add(tags.get(i));
                    }

                    entityManager.persist(post);
                }

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void testFindByReview() {
        Example<PostComment> postExample = Example.of(
            new PostComment()
                .setReview("Spam comment"),
            ExampleMatcher.matching()
                .withIgnorePaths(PostComment_.VOTES)
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT)
        );

        List<PostComment> comments = (List<PostComment>) postCommentRepository.findAll(postExample);
        assertFalse(comments.isEmpty());
    }

    @Test
    public void testFindByPost() {
        PostComment postComment = new PostComment()
            .setPost(
                new Post()
                    .setId(1L)
            );

        List<PostComment> comments = (List<PostComment>) postCommentRepository.findAll(
            Example.of(
                postComment,
                ExampleMatcher.matching()
                    .withIgnorePaths(PostComment_.VOTES)
            )
        );
        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostOrderByCreatedOn() {
        PostComment postComment = new PostComment()
            .setPost(
                new Post()
                    .setId(1L)
            );

        List<PostComment> comments = (List<PostComment>) postCommentRepository.findAll(
            Example.of(
                postComment,
                ExampleMatcher.matching()
                    .withIgnorePaths(PostComment_.VOTES)
            ),
            Sort.by(Sort.Order.asc(PostComment_.CREATED_ON))
        );

        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostAndStatusOrderByCreatedOn() {
        List<PostComment> comments = (List<PostComment>) postCommentRepository.findAll(
            Example.of(
                new PostComment()
                    .setPost(new Post().setId(1L))
                    .setStatus(PostComment.Status.PENDING),
                ExampleMatcher.matching()
                    .withIgnorePaths(PostComment_.VOTES)
            ),
            Sort.by(Sort.Order.asc(PostComment_.CREATED_ON))
        );

        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostAndStatusAndReviewLikeOrderByCreatedOn() {
        PostComment postComment = new PostComment()
            .setPost(new Post().setId(1L))
            .setStatus(PostComment.Status.PENDING)
            .setReview("Spam");

        List<PostComment> comments = (List<PostComment>) postCommentRepository.findAll(
            Example.of(
                postComment,
                ExampleMatcher.matching()
                    .withIgnorePaths(PostComment_.VOTES)
                    .withMatcher(
                        PostComment_.REVIEW,
                        ExampleMatcher.GenericPropertyMatcher::contains
                    )
            ),
            Sort.by(Sort.Order.asc(PostComment_.CREATED_ON))
        );

        assertFalse(comments.isEmpty());
    }

    @Test
    public void testFindByPostAndStatusAndReviewLikeAndVotesGreaterThanEqualOrderByCreatedOn() {
        String reviewPattern = "Awesome";
        int votes = 0;

        PostComment postComment = new PostComment()
            .setPost(new Post().setId(1L))
            .setStatus(PostComment.Status.PENDING)
            .setReview(reviewPattern)
            .setVotes(votes);

        List<PostComment> comments = (List<PostComment>) postCommentRepository.findAll(
            Example.of(
                postComment,
                ExampleMatcher.matching()
                    .withMatcher(PostComment_.REVIEW, ExampleMatcher.GenericPropertyMatcher::contains)
            ),
            Sort.by(Sort.Order.asc(PostComment_.CREATED_ON))
        );
    }

    @Test
    public void testFindBy() {
        String reviewPattern = "Awesome";
        int pageSize = 10;

        PostComment postComment = new PostComment()
            .setPost(new Post().setId(1L))
            .setStatus(PostComment.Status.PENDING)
            .setReview(reviewPattern);

        Page<PostComment> comments = postCommentRepository.findBy(
            Example.of(
                postComment,
                ExampleMatcher.matching()
                    .withIgnorePaths(PostComment_.VOTES)
                    .withMatcher(PostComment_.REVIEW, ExampleMatcher.GenericPropertyMatcher::contains)
            ),
            q -> q
                .sortBy(Sort.by(PostComment_.CREATED_ON).ascending())
                .page(Pageable.ofSize(pageSize))
        );
    }
}

