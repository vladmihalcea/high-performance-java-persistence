package com.vladmihalcea.hpjp.spring.data.query.specification;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.query.specification.config.SpringDataJPASpecificationConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.specification.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.specification.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.specification.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.query.specification.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.query.specification.repository.PostRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.vladmihalcea.hpjp.spring.data.query.specification.repository.PostCommentRepository.Specs.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPASpecificationConfiguration.class)
public class SpringDataJPASpecificationTest extends AbstractSpringTest {

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
    public void testFindByPost() {
        Post post = postRepository.getReferenceById(1L);

        List<PostComment> comments = postCommentRepository.findAll(
            byPost(post)
        );
        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostOrderByCreatedOn() {
        Post post = postRepository.getReferenceById(1L);

        List<PostComment> comments = postCommentRepository.findAll(
            orderByCreatedOn(
                byPost(post)
            )
        );

        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostAndStatusOrderByCreatedOn() {
        Post post = postRepository.getReferenceById(1L);

        List<PostComment> comments = postCommentRepository.findAll(
            orderByCreatedOn(
                byPost(post)
                    .and(byStatus(PostComment.Status.PENDING))
            )
        );

        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostAndStatusAndReviewLikeOrderByCreatedOn() {
        Post post = postRepository.getReferenceById(1L);
        String reviewPattern = "Spam%";

        List<PostComment> comments = postCommentRepository.findAll(
            orderByCreatedOn(
                byPost(post)
                    .and(byStatus(PostComment.Status.PENDING))
                    .and(byReviewLike(reviewPattern))
            )
        );

        assertFalse(comments.isEmpty());
    }

    @Test
    public void testFindByPostAndStatusAndReviewLikeAndVotesGreaterThanEqualOrderByCreatedOn() {
        Post post = postRepository.getReferenceById(1L);
        String reviewPattern = "Awesome%";
        int minVotes = 1;

        List<PostComment> comments = postCommentRepository.findAll(
            orderByCreatedOn(
                byPost(post)
                    .and(byStatus(PostComment.Status.PENDING))
                    .and(byReviewLike(reviewPattern))
                    .and(byVotesGreaterThanEqual(minVotes))
            )
        );

        assertFalse(comments.isEmpty());
    }
}

