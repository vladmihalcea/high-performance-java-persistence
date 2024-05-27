package com.vladmihalcea.hpjp.spring.data.recursive;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.recursive.config.SpringDataJPARecursiveConfiguration;
import com.vladmihalcea.hpjp.spring.data.recursive.domain.Post;
import com.vladmihalcea.hpjp.spring.data.recursive.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.recursive.domain.PostCommentDTO;
import com.vladmihalcea.hpjp.spring.data.recursive.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.recursive.service.ForumService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPARecursiveConfiguration.class)
public class SpringDataJPARecursiveTest extends AbstractSpringTest {

    public static final int POST_COUNT = 50;
    public static final int PAGE_SIZE = 25;
    public static final int TOP_N_HIERARCHY = 3;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                Post post = new Post()
                    .setId(1L)
                    .setTitle("Post 1");

                PostComment comment1 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 13, 12, 23, 5)))
                    .setScore(1)
                    .setReview("Comment 1");

                PostComment comment1_1 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 14, 13, 23, 10)))
                    .setScore(2)
                    .setReview("Comment 1.1")
                    .setParent(comment1);

                PostComment comment1_2 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 14, 15, 45, 15)))
                    .setScore(2)
                    .setParent(comment1)
                    .setReview("Comment 1.2");

                PostComment comment1_2_1 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 15, 10, 15, 20)))
                    .setScore(1)
                    .setReview("Comment 1.2.1")
                    .setParent(comment1_2);

                PostComment comment2 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 13, 15, 23, 25)))
                    .setScore(1)
                    .setReview("Comment 2");

                PostComment comment2_1 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 14, 11, 23, 30)))
                    .setScore(1)
                    .setReview("Comment 2.1")
                    .setParent(comment2);

                PostComment comment2_2 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 14, 14, 45, 35)))
                    .setScore(1)
                    .setReview("Comment 2.2")
                    .setParent(comment2);

                PostComment comment3 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 15, 10, 15, 40)))
                    .setScore(1)
                    .setReview("Comment 3");

                PostComment comment3_1 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 16, 11, 15, 45)))
                    .setScore(10)
                    .setReview("Comment 3.1")
                    .setParent(comment3);

                PostComment comment3_2 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 17, 18, 30, 50)))
                    .setScore(-2)
                    .setReview("Comment 3.2")
                    .setParent(comment3);

                PostComment comment4 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 19, 21, 43, 55)))
                    .setReview("Comment 4")
                    .setScore(-5);

                PostComment comment5 = new PostComment()
                    .setPost(post)
                    .setCreatedOn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 22, 23, 45, 0)))
                    .setReview("Comment 5");

                entityManager.persist(post);
                entityManager.persist(comment1);
                entityManager.persist(comment1_1);
                entityManager.persist(comment1_2);
                entityManager.persist(comment1_2_1);
                entityManager.persist(comment2);
                entityManager.persist(comment2_1);
                entityManager.persist(comment2_2);
                entityManager.persist(comment3);
                entityManager.persist(comment3_1);
                entityManager.persist(comment3_2);
                entityManager.persist(comment4);
                entityManager.persist(comment5);
                
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void testAggregateUsingJava() {
        List<PostCommentDTO> postCommentRoots = forumService.findTopCommentHierarchiesByPostUsingJava(1L, TOP_N_HIERARCHY);

        assertEquals(3, postCommentRoots.size());

        assertEquals(9, postCommentRoots.get(0).getTotalScore());
        assertEquals(6, postCommentRoots.get(1).getTotalScore());
        assertEquals(3, postCommentRoots.get(2).getTotalScore());
    }

    @Test
    public void testAggregateUsingSQL() {
        List<PostCommentDTO> postCommentRoots = forumService.findTopCommentHierarchiesByPostUsingSQL(1L, TOP_N_HIERARCHY);

        assertEquals(3, postCommentRoots.size());

        assertEquals(9, postCommentRoots.get(0).getTotalScore());
        assertEquals(6, postCommentRoots.get(1).getTotalScore());
        assertEquals(3, postCommentRoots.get(2).getTotalScore());
    }
}

