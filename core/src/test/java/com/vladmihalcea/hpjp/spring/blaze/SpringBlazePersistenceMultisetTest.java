package com.vladmihalcea.hpjp.spring.blaze;

import com.vladmihalcea.hpjp.spring.blaze.config.SpringBlazePersistenceConfiguration;
import com.vladmihalcea.hpjp.spring.blaze.domain.*;
import com.vladmihalcea.hpjp.spring.blaze.domain.views.PostCommentView;
import com.vladmihalcea.hpjp.spring.blaze.domain.views.PostWithCommentsAndTagsView;
import com.vladmihalcea.hpjp.spring.blaze.service.ForumService;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import org.hibernate.loader.MultipleBagFetchException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringBlazePersistenceConfiguration.class)
public class SpringBlazePersistenceMultisetTest extends AbstractSpringTest {

    public static final int POST_COUNT = 50;
    public static final int POST_COMMENT_COUNT = 20;
    public static final int TAG_COUNT = 10;
    public static final int VOTE_COUNT = 5;

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            UserVote.class,
            PostComment.class,
            Post.class,
            Tag.class,
            User.class,
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                User alice = new User()
                    .setId(1L)
                    .setFirstName("Alice")
                    .setLastName("Smith");

                User bob = new User()
                    .setId(2L)
                    .setFirstName("Bob")
                    .setLastName("Johnson");

                entityManager.persist(alice);
                entityManager.persist(bob);

                List<Tag> tags = new ArrayList<>();

                for (long i = 1; i <= TAG_COUNT; i++) {
                    Tag tag = new Tag()
                        .setId(i)
                        .setName(String.format("Tag nr. %d", i));

                    entityManager.persist(tag);
                    tags.add(tag);
                }

                long commentId = 0;
                long voteId = 0;

                for (long postId = 1; postId <= POST_COUNT; postId++) {
                    Post post = new Post()
                        .setId(postId)
                        .setTitle(String.format("Post nr. %d", postId));


                    for (long i = 0; i < POST_COMMENT_COUNT; i++) {
                        PostComment comment = new PostComment()
                            .setId(++commentId)
                            .setReview("Excellent!");

                        for (int j = 0; j < VOTE_COUNT; j++) {
                            comment.addVote(
                                new UserVote()
                                    .setId(++voteId)
                                    .setScore(Math.random() > 0.5 ? 1 : -1)
                                    .setUser(Math.random() > 0.5 ? alice : bob)
                            );
                        }

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
    public void testWithCartesianProduct() {
        transactionTemplate.execute(transactionStatus -> {
            try {
                List<Post> posts = entityManager.createQuery("""
                    select p
                    from Post p
                    left join fetch p.tags t
                    left join fetch p.comments pc
                    left join fetch pc.votes v
                    left join fetch v.user u
                    where p.id between :minId and :maxId
                    """, Post.class)
                .setParameter("minId", 1L)
                .setParameter("maxId", 50L)
                .getResultList();

                fail("Should have thrown MultipleBagFetchException");
            } catch (IllegalArgumentException e) {
                LOGGER.info("Expected", e);
                assertEquals(MultipleBagFetchException.class, ExceptionUtil.rootCause(e).getClass());
            }
            return null;
        });
    }

    @Test
    public void testWithSuccessiveJoinFetch() {
        List<Post> posts = forumService.findWithCommentsAndTagsByIds(
            1L, 50L
        );

        assertEquals(POST_COUNT, posts.size());

        for (Post post : posts) {
            assertEquals(POST_COMMENT_COUNT, post.getComments().size());
            for(PostComment comment : post.getComments()) {
                assertEquals(VOTE_COUNT, comment.getVotes().size());
            }
            assertEquals(TAG_COUNT, post.getTags().size());
        }
    }

    @Test
    public void testWithMultiset() {
        List<PostWithCommentsAndTagsView> posts = forumService.findPostWithCommentsAndTagsViewByIds(
            1L, 50L
        );

        assertEquals(POST_COUNT, posts.size());

        for (PostWithCommentsAndTagsView post : posts) {
            assertEquals(POST_COMMENT_COUNT, post.getComments().size());
            for(PostCommentView comment : post.getComments()) {
                assertEquals(VOTE_COUNT, comment.getVotes().size());
            }
            assertEquals(TAG_COUNT, post.getTags().size());
        }
    }

}

