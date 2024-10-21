package com.vladmihalcea.hpjp.spring.data.bidirectional;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.bidirectional.config.SpringDataJPABidirectionalConfiguration;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.Post;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.PostDetails;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.bidirectional.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.bidirectional.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.bidirectional.service.ForumService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionCallback;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPABidirectionalConfiguration.class)
public class SpringDataJPABidirectionalTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            PostComment.class,
            PostDetails.class,
            Post.class,
            Tag.class
        };
    }

    @Override
    public void afterInit() {
        postRepository.persist(
            new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"))
                .addComment(
                    new PostComment()
                        .setReview("Best book on JPA and Hibernate!")
                )
                .addComment(
                    new PostComment()
                        .setReview("A must-read for every Java developer!")
                )
                .addTag(new Tag().setName("JDBC"))
                .addTag(new Tag().setName("Hibernate"))
                .addTag(new Tag().setName("jOOQ"))
        );
    }

    @Test
    public void testPersistPostCommentWithoutPostFetching() {
        LOGGER.info("Persisting PostComment without fetching the Post entity");
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            postCommentRepository.persist(
                new PostComment()
                    .setPost(postRepository.getReferenceById(1L))
                    .setReview("Very informative. Learned a lot, applied every day.")
            );

            return null;
        });
    }

    @Test
    public void testPersistPostCommentWithPostFetching() {
        LOGGER.info("Persisting PostComment when the Post entity was already fetched");
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = postRepository.findByIdWithDetailsAndComments(1L);

            post.addComment(
                new PostComment()
                    .setReview("Very informative. Learned a lot, applied every day.")
            );

            return null;
        });
    }

    @Test
    public void testDelete() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            postRepository.deleteById(1L);

            return null;
        });
    }

    @Test
    public void testSaveAndRemoveChildEntityWithoutParentFetching() {
        LOGGER.info("Add PostComment to Post");
        PostComment comment = forumService.addPostComment("Best book on JPA and Hibernate!", 1L);

        LOGGER.info("Remove PostComment from Post");
        forumService.removePostComment(comment.getId());
    }

    @Test
    public void testSaveAndRemoveChildEntityAntiPattern() {
        LOGGER.info("Add PostComment to Post");
        PostComment comment = forumService.addPostCommentAntiPattern("Best book on JPA and Hibernate!", 1L);

        LOGGER.info("Remove PostComment from Post");
        forumService.removePostCommentAntiPattern(comment.getId());
    }
}

